package depth.finvibe.investment.modules.market.infra.websocket.kis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.market.application.port.in.CurrentPriceCommandUseCase;
import depth.finvibe.investment.modules.market.dto.CurrentPriceUpdatedEvent;
import depth.finvibe.investment.modules.market.infra.config.KisCredentialsProperties;
import depth.finvibe.investment.modules.market.infra.websocket.kis.model.KisMessage;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class KisConnectionPool {

    private static final int MAX_SUBSCRIPTIONS_PER_SESSION = 41;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private final Map<String, KisWebsocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> symbolToStockId = new ConcurrentHashMap<>();
    private final Map<Long, String> stockIdToSymbol = new ConcurrentHashMap<>();

    private final KisCredentialsProperties properties;
    private final ObjectMapper objectMapper;
    private final CurrentPriceCommandUseCase currentPriceCommandUseCase;

    public KisConnectionPool(
            KisCredentialsProperties properties,
            ObjectMapper objectMapper,
            CurrentPriceCommandUseCase currentPriceCommandUseCase
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.currentPriceCommandUseCase = currentPriceCommandUseCase;
    }

    /**
     * KIS WebSocket 세션을 초기화합니다.
     * 애플리케이션 시작 시 자동으로 호출되지 않으며, 필요 시 명시적으로 호출해야 합니다.
     */
    public void initializeSessions() {
        log.info("KIS WebSocket 세션 초기화 시작 - Credential 수: {}", properties.credentials().size());

        List<KisCredentialsProperties.Credential> validCredentials = properties.credentials().stream()
                .filter(this::isValidCredential)
                .toList();

        if (validCredentials.isEmpty()) {
            log.warn("유효한 KIS Credential이 없습니다. WebSocket 세션을 생성하지 않습니다.");
            return;
        }

        validCredentials.forEach(credential ->
                tryRegisterSession(credential.appKey(), credential.appSecret())
        );

        log.info("KIS WebSocket 세션 초기화 완료 - 등록 시도: {}", validCredentials.size());
    }

    private boolean isValidCredential(KisCredentialsProperties.Credential credential) {
        return credential.appKey() != null && !credential.appKey().isBlank()
                && credential.appSecret() != null && !credential.appSecret().isBlank();
    }

    public void tryRegisterSession(String appKey, String appSecret) {
        KisWebSocketApprovalKeyClient approvalKeyClient =
                new KisWebSocketApprovalKeyClient(appKey, appSecret, properties.baseUrl());

        String approvalKey = approvalKeyClient.requestApprovalKey();
        KisWebsocketSession newSession = new KisWebsocketSession(approvalKey, this::onPriceUpdated, objectMapper);

        CompletableFuture<KisWebsocketSession> connectFuture = newSession.connect(properties.websocket().url());
        connectFuture
                .thenAccept(session -> handleSessionRegistrationSuccess(appKey, session))
                .exceptionally(ex -> handleSessionRegistrationFailure(appKey, ex));
    }

    public void subscribe(Long stockId, String symbol) {
        // 이미 구독 중인 경우 중복 구독 방지 (멱등성 보장)
        if (isSubscribed(stockId)) {
            log.trace("이미 구독 중인 종목 - stockId: {}, symbol: {}", stockId, symbol);
            return;
        }

        symbolToStockId.put(symbol, stockId);
        stockIdToSymbol.put(stockId, symbol);

        KisWebsocketSession targetSession = findSessionWithAvailableSlot();
        if (targetSession == null) {
            log.error("구독 가능한 KIS WebSocket 세션이 없습니다. - stockId: {}, symbol: {}", stockId, symbol);
            // 매핑 정보 롤백
            symbolToStockId.remove(symbol);
            stockIdToSymbol.remove(stockId);
            return;
        }

        try {
            targetSession.subscribe(symbol);
            log.debug("KIS WebSocket 종목 구독 성공 - stockId: {}, symbol: {}, 현재 구독 수: {}",
                    stockId, symbol, targetSession.getSubscriptionCount());
        } catch (Exception ex) {
            log.error("KIS WebSocket 종목 구독 실패 - stockId: {}, symbol: {}", stockId, symbol, ex);
            // 구독 실패 시 매핑 정보 롤백
            symbolToStockId.remove(symbol);
            stockIdToSymbol.remove(stockId);
            throw ex;
        }
    }

    public void unsubscribe(Long stockId, String symbol) {
        symbolToStockId.remove(symbol);
        stockIdToSymbol.remove(stockId);

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

    /**
     * 현재 구독 중인 모든 종목 ID를 반환합니다.
     *
     * @return 구독 중인 종목 ID Set
     */
    public Set<Long> getSubscribedStockIds() {
        return new HashSet<>(stockIdToSymbol.keySet());
    }

  /**
   * 특정 종목이 이미 구독 중인지 확인합니다.
   *
   * @param stockId 종목 ID
   * @return 구독 중이면 true, 아니면 false
   */
  public boolean isSubscribed(Long stockId) {
    return stockIdToSymbol.containsKey(stockId);
  }

  /**
   * 현재 사용 가능한 세션 수를 반환합니다.
   *
   * @return 등록된 세션 수
   */
  public int getAvailableSessionCount() {
    return sessions.size();
  }
}
