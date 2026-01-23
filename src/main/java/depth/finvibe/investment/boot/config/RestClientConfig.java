package depth.finvibe.investment.boot.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import depth.finvibe.investment.modules.market.infra.client.tokenmanage.KisTokenManager;
import depth.finvibe.investment.modules.market.infra.config.KisCredentialsProperties;
import depth.finvibe.investment.modules.market.infra.config.KisCredentialsProperties.Credential;

@Configuration
public class RestClientConfig {
    private final String appKey;
    private final String appSecret;

    public RestClientConfig(KisCredentialsProperties kisProperties) {
        List<Credential> validCredentials = kisProperties.getValidCredentials();
        if (validCredentials.isEmpty()) {
            throw new IllegalStateException("최소 하나의 유효한 KIS credential이 필요합니다");
        }
        Credential firstCredential = validCredentials.getFirst();
        this.appKey = firstCredential.appKey();
        this.appSecret = firstCredential.appSecret();
    }

    @Bean
    @Qualifier("kisRestClient")
    public RestClient kisRestClient(
            KisTokenManager kisTokenManager
    ) {
        return RestClient.builder()
                .baseUrl("https://openapi.koreainvestment.com:9443")
                .defaultHeaders(headers -> {
                    headers.add("Content-Type", "application/json");
                    headers.add("appkey", appKey);
                    headers.add("appsecret", appSecret);
                    headers.add("custtype", "P");
                })
                .requestInterceptor((request, body, execution) -> {
                    String accessToken = kisTokenManager.getAccessToken();

                    if (accessToken == null) {
                        throw new IOException("Failed to obtain access token for KIS API");
                    }

                    request.getHeaders().add("Authorization", "Bearer " + accessToken);
                    return execution.execute(request, body);
                })
                .build();
    }
}
