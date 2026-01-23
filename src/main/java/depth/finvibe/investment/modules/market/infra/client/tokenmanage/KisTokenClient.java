package depth.finvibe.investment.modules.market.infra.client.tokenmanage;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import depth.finvibe.investment.modules.market.infra.config.KisCredentialsProperties;
import depth.finvibe.investment.modules.market.infra.config.KisCredentialsProperties.Credential;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
public class KisTokenClient {
    private final String apiKey;
    private final String apiSecret;
    private final RestClient tokenClient;

    public KisTokenClient(KisCredentialsProperties kisProperties) {
        List<Credential> validCredentials = kisProperties.getValidCredentials();

        if (validCredentials.isEmpty()) {
            throw new IllegalStateException("최소 하나의 유효한 KIS credential이 필요합니다");
        }

        Credential firstCredential = validCredentials.getFirst();

        this.apiKey = firstCredential.appKey();
        this.apiSecret = firstCredential.appSecret();
        this.tokenClient = RestClient.builder()
                .baseUrl("https://openapi.koreainvestment.com:9443")
                .build();
    }

    public TokenResponse requestAccessToken() {
        KisTokenResponse response = tokenClient.post()
                .uri("/oauth2/tokenP")
                .body(
                        KisTokenRequest.builder()
                                .grant_type("client_credentials")
                                .appkey(apiKey)
                                .appsecret(apiSecret)
                                .build()
                )
                .retrieve()
                .body(KisTokenResponse.class);

        KisTokenResponse safeResponse = Objects.requireNonNull(response);
        return new TokenResponse(
                safeResponse.getAccess_token(),
                safeResponse.getExpires_in().longValue()
        );
    }

    public record TokenResponse(String accessToken, long expiresIn) {
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class KisTokenRequest {
        private String grant_type;
        private String appkey;
        private String appsecret;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class KisTokenResponse {
        private String access_token;
        private String token_type;
        private Float expires_in;
        private String access_token_token_expired;
    }
}
