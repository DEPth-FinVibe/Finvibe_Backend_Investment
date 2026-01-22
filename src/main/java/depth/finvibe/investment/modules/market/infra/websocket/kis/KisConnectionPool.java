package depth.finvibe.investment.modules.market.infra.websocket.kis;

import depth.finvibe.investment.modules.market.application.port.in.CurrentPriceCommandUseCase;
import depth.finvibe.investment.modules.market.dto.CurrentPriceUpdatedEvent;
import depth.finvibe.investment.modules.market.infra.websocket.kis.model.KisMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class KisConnectionPool {
    // <AppKey, Session>
    private final Map<String, KisWebsocketSession> sessions = new ConcurrentHashMap<>();

    private final String KIS_APPROVAL_BASE_URL;
    private final String KIS_WEBSOCKET_BASE_URL;

    private final ObjectMapper objectMapper;
    private final CurrentPriceCommandUseCase currentPriceCommandUseCase;

    public KisConnectionPool(
            @Value("${market.kis.base-url:https://openapi.koreainvestment.com:9443}")
            String baseUrl,
            @Value("${market.kis.websocket.url:wss://openapi.koreainvestment.com:9443/websocket}")
            String websocketUrl,
            ObjectMapper objectMapper,
            CurrentPriceCommandUseCase currentPriceCommandUseCase) {
        this.KIS_APPROVAL_BASE_URL = baseUrl;
        this.KIS_WEBSOCKET_BASE_URL = websocketUrl;
        this.objectMapper = objectMapper;
        this.currentPriceCommandUseCase = currentPriceCommandUseCase;
    }

    public void tryRegisterSession(String appKey, String appSecret) {
        KisWebSocketApprovalKeyClient approvalKeyClient = new KisWebSocketApprovalKeyClient(appKey, appSecret, KIS_APPROVAL_BASE_URL);

        String approvalKey = approvalKeyClient.requestApprovalKey();

        KisWebsocketSession newSession = new KisWebsocketSession(approvalKey, this::onPriceUpdated, objectMapper);

        CompletableFuture<KisWebsocketSession> connectFuture = newSession.connect(KIS_WEBSOCKET_BASE_URL);
        connectFuture.thenAccept( s-> {
            sessions.put(appKey, s);
            log.info("KIS WebSocket 세션 등록 성공 - AppKey: {}", appKey);
        }).exceptionally(ex -> {
            log.error("KIS WebSocket 세션 등록 실패 - AppKey: {}", appKey, ex);
            return null;
        });
    }

    private void onPriceUpdated(KisMessage.TransactionResponse response) {
        String symbol = response.getShortStockCode(); //ISCD 코드

        //TODO: stockId를 알아내서 비즈니스 로직 실행
    }
}
