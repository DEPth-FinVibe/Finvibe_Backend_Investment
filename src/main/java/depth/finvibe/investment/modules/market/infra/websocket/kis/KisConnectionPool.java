package depth.finvibe.investment.modules.market.infra.websocket.kis;

import depth.finvibe.investment.modules.market.application.port.in.CurrentPriceCommandUseCase;
import depth.finvibe.investment.modules.market.dto.CurrentPriceUpdatedEvent;
import depth.finvibe.investment.modules.market.infra.websocket.kis.model.KisMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class KisConnectionPool {

    private static final int MAX_SUBSCRIPTIONS_PER_SESSION = 41;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private final Map<String, KisWebsocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> symbolToStockId = new ConcurrentHashMap<>();

    private final String approvalBaseUrl;
    private final String websocketBaseUrl;
    private final ObjectMapper objectMapper;
    private final CurrentPriceCommandUseCase currentPriceCommandUseCase;

    public KisConnectionPool(
            @Value("${market.kis.base-url:https://openapi.koreainvestment.com:9443}")
            String approvalBaseUrl,
            @Value("${market.kis.websocket.url:wss://openapi.koreainvestment.com:9443/websocket}")
            String websocketBaseUrl,
            ObjectMapper objectMapper,
            CurrentPriceCommandUseCase currentPriceCommandUseCase) {
        this.approvalBaseUrl = approvalBaseUrl;
        this.websocketBaseUrl = websocketBaseUrl;
        this.objectMapper = objectMapper;
        this.currentPriceCommandUseCase = currentPriceCommandUseCase;
    }

    public void tryRegisterSession(String appKey, String appSecret) {
        KisWebSocketApprovalKeyClient approvalKeyClient =
                new KisWebSocketApprovalKeyClient(appKey, appSecret, approvalBaseUrl);

        String approvalKey = approvalKeyClient.requestApprovalKey();
        KisWebsocketSession newSession = new KisWebsocketSession(approvalKey, this::onPriceUpdated, objectMapper);

        CompletableFuture<KisWebsocketSession> connectFuture = newSession.connect(websocketBaseUrl);
        connectFuture
                .thenAccept(session -> handleSessionRegistrationSuccess(appKey, session))
                .exceptionally(ex -> handleSessionRegistrationFailure(appKey, ex));
    }

    public void subscribe(Long stockId, String symbol) {
        symbolToStockId.put(symbol, stockId);

        KisWebsocketSession targetSession = findSessionWithAvailableSlot();
        if (targetSession == null) {
            log.error("구독 가능한 KIS WebSocket 세션이 없습니다. - stockId: {}, symbol: {}", stockId, symbol);
            return;
        }

        targetSession.subscribe(symbol);
        log.debug("KIS WebSocket 종목 구독 성공 - stockId: {}, symbol: {}, 현재 구독 수: {}",
                stockId, symbol, targetSession.getSubscriptionCount());
    }

    public void unsubscribe(Long stockId, String symbol) {
        symbolToStockId.remove(symbol);

        KisWebsocketSession targetSession = findSessionAndUnsubscribe(symbol);
        if (targetSession == null) {
            log.error("구독 해제 가능한 KIS WebSocket 세션이 없습니다. - stockId: {}, symbol: {}", stockId, symbol);
            return;
        }

        log.debug("KIS WebSocket 종목 구독 해제 성공 - stockId: {}, symbol: {}, 현재 구독 수: {}",
                stockId, symbol, targetSession.getSubscriptionCount());
    }

    private void handleSessionRegistrationSuccess(String appKey, KisWebsocketSession session) {
        sessions.put(appKey, session);
        log.info("KIS WebSocket 세션 등록 성공 - AppKey: {}", appKey);
    }

    private Void handleSessionRegistrationFailure(String appKey, Throwable ex) {
        log.error("KIS WebSocket 세션 등록 실패 - AppKey: {}", appKey, ex);
        return null;
    }

    private KisWebsocketSession findSessionWithAvailableSlot() {
        return sessions.values().stream()
                .filter(session -> session.getSubscriptionCount() < MAX_SUBSCRIPTIONS_PER_SESSION)
                .min(Comparator.comparingInt(KisWebsocketSession::getSubscriptionCount))
                .orElse(null);
    }

    private KisWebsocketSession findSessionAndUnsubscribe(String symbol) {
        return sessions.values().stream()
                .filter(session -> session.getSubscriptionCount() > 0)
                .filter(session -> tryUnsubscribe(session, symbol))
                .findFirst()
                .orElse(null);
    }

    private boolean tryUnsubscribe(KisWebsocketSession session, String symbol) {
        try {
            session.unsubscribe(symbol);
            return true;
        } catch (IllegalStateException ex) {
            log.error("KIS WebSocket 세션에서 구독 해제 실패 - symbol: {}, session subscriptions: {}",
                    symbol, session.getSubscriptionCount(), ex);
            return false;
        }
    }

    private void onPriceUpdated(KisMessage.TransactionResponse response) {
        String symbol = response.getShortStockCode();

        Long stockId = symbolToStockId.get(symbol);
        if (stockId == null) {
            log.warn("수신된 가격 정보의 종목 ID를 찾을 수 없습니다. - symbol: {}", symbol);
            return;
        }

        CurrentPriceUpdatedEvent event = mapToEvent(response, stockId);
        currentPriceCommandUseCase.stockPriceUpdated(event);
    }

    private CurrentPriceUpdatedEvent mapToEvent(KisMessage.TransactionResponse response, Long stockId) {
        LocalDateTime timestamp = parseTimestamp(response.getBusinessDate(), response.getStockExecutionTime());

        return CurrentPriceUpdatedEvent.builder()
                .stockId(stockId)
                .at(timestamp)
                .open(toBigDecimal(response.getOpenStockPrice()))
                .high(toBigDecimal(response.getHighStockPrice()))
                .low(toBigDecimal(response.getLowStockPrice()))
                .close(toBigDecimal(response.getCurrentStockPrice()))
                .prevDayChangePct(toBigDecimal(response.getPreviousDayChangeRate()))
                .volume(toBigDecimal(response.getCumulativeVolume()))
                .value(toBigDecimal(response.getCumulativeTradingAmount()))
                .build();
    }

    private LocalDateTime parseTimestamp(String businessDate, String stockExecutionTime) {
        LocalDate date = LocalDate.parse(businessDate, DATE_FORMATTER);
        LocalTime time = LocalTime.parse(stockExecutionTime, TIME_FORMATTER);
        return LocalDateTime.of(date, time);
    }

    private BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
