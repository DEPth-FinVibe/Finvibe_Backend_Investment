package depth.finvibe.investment.boot.config;

import depth.finvibe.investment.modules.market.infra.client.tokenmanage.KisTokenManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Configuration
public class RestClientConfig {
    private final String appKey;
    private final String appSecret;

    public RestClientConfig(
            @Value("${market.kis.api-key}") String apiKey,
            @Value("${market.kis.api-secret}") String apiSecret
    ) {
        this.appKey = apiKey;
        this.appSecret = apiSecret;
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

                    if(accessToken == null) {
                        throw new IOException("Failed to obtain access token for KIS API");
                    }

                    request.getHeaders().add("Authorization", "Bearer " + accessToken);
                    return execution.execute(request, body);
                })
                .build();
    }
}
