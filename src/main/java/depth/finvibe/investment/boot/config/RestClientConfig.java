package depth.finvibe.investment.boot.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import depth.finvibe.investment.modules.market.infra.client.KisCredentialAllocator;
import depth.finvibe.investment.modules.market.infra.client.KisRateLimiter;
import depth.finvibe.investment.modules.market.infra.client.tokenmanage.KisTokenManager;
import depth.finvibe.investment.modules.market.infra.config.KisCredentialsProperties.Credential;

@Configuration
public class RestClientConfig {
  private final KisCredentialAllocator credentialAllocator;
  private final KisRateLimiter rateLimiter;
  private final KisTokenManager tokenManager;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public RestClientConfig(
      KisCredentialAllocator credentialAllocator,
      KisRateLimiter rateLimiter,
      KisTokenManager tokenManager
  ) {
    this.credentialAllocator = credentialAllocator;
    this.rateLimiter = rateLimiter;
    this.tokenManager = tokenManager;
  }

  @Bean
  @Qualifier("kisRestClient")
  public RestClient kisRestClient() {
    return RestClient.builder()
        .baseUrl("https://openapi.koreainvestment.com:9443")
        .defaultHeaders(headers -> {
          headers.add("Content-Type", "application/json");
          headers.add("custtype", "P");
        })
        .requestInterceptor((request, body, execution) -> {
          Credential credential = credentialAllocator.selectCredentialForRequest(rateLimiter);
          String accessToken = tokenManager.getAccessToken(credential);

          if (accessToken == null) {
            throw new IOException("Failed to obtain access token for KIS API");
          }

          request.getHeaders().set("appkey", credential.appKey());
          request.getHeaders().set("appsecret", credential.appSecret());
          request.getHeaders().add("Authorization", "Bearer " + accessToken);

          ClientHttpResponse response = execution.execute(request, body);

          // 응답을 캐싱하고 msg_cd 확인
          return new CachedBodyClientHttpResponse(response, credential.appKey(), rateLimiter, objectMapper);
        })
        .build();
  }

  /**
   * 응답 본문을 캐싱하여 여러 번 읽을 수 있도록 하는 래퍼 클래스
   * msg_cd를 확인하여 레이트 리미트 에러를 감지합니다.
   */
  private static class CachedBodyClientHttpResponse implements ClientHttpResponse {
    private final ClientHttpResponse response;
    private final byte[] cachedBody;

    public CachedBodyClientHttpResponse(
        ClientHttpResponse response,
        String appKey,
        KisRateLimiter rateLimiter,
        ObjectMapper objectMapper
    ) throws IOException {
      this.response = response;
      this.cachedBody = response.getBody().readAllBytes();

      // msg_cd 확인하여 레이트 리미트 에러 처리
      try {
        JsonNode root = objectMapper.readTree(cachedBody);
        JsonNode msgCdNode = root.get("msg_cd");

        if (msgCdNode != null && "EGW00201".equals(msgCdNode.asText())) {
          rateLimiter.markAsExceeded(appKey);
        }
      } catch (Exception e) {
        // 파싱 실패 시 무시 (JSON이 아니거나 msg_cd가 없는 경우)
      }
    }

    @Override
    public HttpStatusCode getStatusCode() throws IOException {
      return response.getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
      return response.getStatusText();
    }

    @Override
    public void close() {
      response.close();
    }

    @Override
    public InputStream getBody() throws IOException {
      return new ByteArrayInputStream(cachedBody);
    }

    @Override
    public HttpHeaders getHeaders() {
      return response.getHeaders();
    }
  }
}
