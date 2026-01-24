package depth.finvibe.investment.boot.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import depth.finvibe.investment.modules.market.infra.client.KisCredentialAllocator;
import depth.finvibe.investment.modules.market.infra.client.KisRateLimiter;
import depth.finvibe.investment.modules.market.infra.client.tokenmanage.KisTokenManager;
import depth.finvibe.investment.modules.market.infra.config.KisCredentialsProperties.Credential;

@Configuration
public class RestClientConfig {
    private final KisCredentialAllocator credentialAllocator;
    private final KisRateLimiter rateLimiter;
    private final KisTokenManager tokenManager;

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
                    return execution.execute(request, body);
                })
                .build();
    }
}
