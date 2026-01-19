package depth.finvibe.investment.modules.market.infra.client;

import jakarta.annotation.PostConstruct;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Component
public class KisTokenManager {
    private final String apiKey;
    private final String apiSecret;
    private final RestClient tokenClient;
    private final TaskScheduler taskScheduler;

    @Getter @Setter(AccessLevel.PRIVATE)
    private String accessToken;

    public KisTokenManager(
        @Value("${market.kis.api-key}") String apiKey,
        @Value("${market.kis.api-secret}") String apiSecret,
        TaskScheduler taskScheduler
    ) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;

        this.tokenClient = RestClient.builder()
                .baseUrl("https://openapi.koreainvestment.com:9443")
                .build();
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        refreshToken();
    }

    public void refreshToken() {
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

        String accessToken = Objects.requireNonNull(response).getAccess_token();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(response.getExpires_in().longValue());
        LocalDateTime nextRefreshTime = calculateNextRefreshTime(expiresAt);

        this.setAccessToken(accessToken);
        taskScheduler.schedule(
                this::refreshToken,
                nextRefreshTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
        );
    }

    private LocalDateTime calculateNextRefreshTime(LocalDateTime expiresAt) {
        // 발급 6시간 후 부터 토큰 Refresh가능. 24시간 후엔 토큰 만료됨.
        LocalDateTime sixHoursLater = LocalDateTime.now().plusHours(6);
        LocalDateTime safeRefreshTime = expiresAt.minusMinutes(10);

        return safeRefreshTime.isBefore(sixHoursLater) ? safeRefreshTime : sixHoursLater;
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
