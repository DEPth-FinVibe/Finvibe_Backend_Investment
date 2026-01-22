package depth.finvibe.investment.modules.market.infra.websocket.kis.legacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class KisWebSocketApprovalClient {
    private final String apiKey;
    private final String apiSecret;
    private final RestClient restClient;

  public KisWebSocketApprovalClient(
          @Value("${market.kis.api-key}") String apiKey,
          @Value("${market.kis.api-secret}") String apiSecret,
          @Value("${market.kis.base-url}") String baseUrl
  ) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    log.info("KIS WebSocket Approval Client 초기화 - baseUrl: {}", baseUrl);
  }

  public String requestApprovalKey() {
    try {
      log.info("KIS Approval Key 요청 시작");
      ApprovalResponse response = restClient.post()
              .uri("/oauth2/Approval")
              .body(
                      ApprovalRequest.builder()
                              .grant_type("client_credentials")
                              .appkey(apiKey)
                              .secretkey(apiSecret)
                              .build()
              )
              .retrieve()
              .body(ApprovalResponse.class);

      if (response == null) {
        log.error("KIS Approval Key 응답이 null입니다.");
        return null;
      }
      
      if (response.getApproval_key() == null || response.getApproval_key().isBlank()) {
        log.error("KIS Approval Key가 비어있습니다. - response: {}", response);
        return null;
      }
      
      log.info("KIS Approval Key 획득 성공 - key length: {}", response.getApproval_key().length());
      return response.getApproval_key();
    } catch (Exception ex) {
      log.error("KIS Approval Key 요청 실패", ex);
      return null;
    }
  }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ApprovalRequest {
        private String grant_type;
        private String appkey;
        private String secretkey;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class ApprovalResponse {
        private String approval_key;
    }
}
