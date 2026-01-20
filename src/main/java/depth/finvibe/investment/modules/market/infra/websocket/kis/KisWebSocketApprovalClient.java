package depth.finvibe.investment.modules.market.infra.websocket.kis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KisWebSocketApprovalClient {
    private final String apiKey;
    private final String apiSecret;
    private final RestClient restClient;

    public KisWebSocketApprovalClient(
            @Value("${market.kis.api-key}") String apiKey,
            @Value("${market.kis.api-secret}") String apiSecret
    ) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.restClient = RestClient.builder()
                .baseUrl("https://openapi.koreainvestment.com:9443")
                .build();
    }

    public String requestApprovalKey() {
        ApprovalResponse response = restClient.post()
                .uri("/oauth2/Approval")
                .body(
                        ApprovalRequest.builder()
                                .grant_type("client_credentials")
                                .appkey(apiKey)
                                .appsecret(apiSecret)
                                .build()
                )
                .retrieve()
                .body(ApprovalResponse.class);

        if (response == null || response.getApproval_key() == null || response.getApproval_key().isBlank()) {
            return null;
        }
        return response.getApproval_key();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ApprovalRequest {
        private String grant_type;
        private String appkey;
        private String appsecret;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class ApprovalResponse {
        private String approval_key;
    }
}
