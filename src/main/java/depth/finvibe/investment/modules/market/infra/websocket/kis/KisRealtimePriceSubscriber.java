package depth.finvibe.investment.modules.market.infra.websocket.kis;

import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.error.WebSocketErrorCode;
import depth.finvibe.investment.shared.error.DomainException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisRealtimePriceSubscriber {
    private static final String TR_ID = "H0STCNT0";
    private static final String CUST_TYPE = "P";

    private final KisWebSocketApprovalClient approvalClient;
    private final KisRealtimePriceMessageHandler messageHandler;
    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper;

    private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> symbolToStockId = new ConcurrentHashMap<>();
    private volatile String approvalKey;

    @Value("${market.kis.websocket.url:ws://ops.koreainvestment.com:21000}")
    private String websocketUrl;

  public synchronized void syncSubscriptions(List<Long> stockIds) {
    log.info("구독 동기화 시작 - 요청된 종목 수: {}", stockIds.size());
    List<Stock> stocks = stockRepository.findAllById(stockIds);
    Set<String> targetSymbols = stocks.stream()
            .map(Stock::getSymbol)
            .filter(symbol -> symbol != null && !symbol.isBlank())
            .collect(Collectors.toSet());

    Map<String, Long> latestMap = stocks.stream()
            .filter(stock -> stock.getSymbol() != null && !stock.getSymbol().isBlank())
            .collect(Collectors.toMap(Stock::getSymbol, Stock::getId, (a, b) -> a));

    Set<String> toSubscribe = new HashSet<>(targetSymbols);
    toSubscribe.removeAll(subscribedSymbols);

    Set<String> toUnsubscribe = new HashSet<>(subscribedSymbols);
    toUnsubscribe.removeAll(targetSymbols);

    log.debug("구독 변경 사항 - 추가: {}, 제거: {}, 현재 구독: {}", 
            toSubscribe.size(), toUnsubscribe.size(), subscribedSymbols.size());

    if (!toSubscribe.isEmpty() || !toUnsubscribe.isEmpty()) {
      ensureConnected();
    }

    for (String symbol : toSubscribe) {
      sendSubscription(symbol, true);
      subscribedSymbols.add(symbol);
      log.debug("종목 구독 완료: {}", symbol);
    }

    for (String symbol : toUnsubscribe) {
      sendSubscription(symbol, false);
      subscribedSymbols.remove(symbol);
      log.debug("종목 구독 해제 완료: {}", symbol);
    }

    symbolToStockId.clear();
    symbolToStockId.putAll(latestMap);

    if (subscribedSymbols.isEmpty()) {
      log.info("모든 구독이 해제되어 WebSocket 연결을 종료합니다.");
      closeConnection();
    } else {
      log.info("구독 동기화 완료 - 활성 구독: {}", subscribedSymbols.size());
    }
  }

  private void ensureConnected() {
    if (webSocketRef.get() != null) {
      log.debug("WebSocket이 이미 연결되어 있습니다.");
      return;
    }
    if (!connecting.compareAndSet(false, true)) {
      log.debug("WebSocket 연결 시도 중입니다.");
      return;
    }
    try {
      log.info("KIS WebSocket 연결 시작 - URL: {}", websocketUrl);
      String approvalKey = approvalClient.requestApprovalKey();
      if (approvalKey == null) {
        log.error("Approval Key 획득 실패 - KIS API 자격증명을 확인하세요");
        throw new DomainException(WebSocketErrorCode.UNAUTHORIZED);
      }
      log.info("Approval Key 획득 성공 - key: {}...", approvalKey.substring(0, Math.min(10, approvalKey.length())));
      this.approvalKey = approvalKey;
      
      log.debug("WebSocket 연결 중 - URL: {}", websocketUrl);
      WebSocket webSocket = HttpClient.newHttpClient()
              .newWebSocketBuilder()
              .connectTimeout(Duration.ofSeconds(5))
              .buildAsync(URI.create(websocketUrl), new KisWebSocketListener())
              .join();
      webSocketRef.set(webSocket);
      log.info("KIS WebSocket 연결 완료");
    } catch (java.util.concurrent.CompletionException ex) {
      log.error("KIS WebSocket 연결 실패 - Handshake 오류", ex);
      if (ex.getCause() instanceof java.net.http.WebSocketHandshakeException) {
        log.error("WebSocket Handshake 실패 - KIS API approval key 또는 WebSocket URL을 확인하세요");
        log.error("현재 WebSocket URL: {}", websocketUrl);
        log.error("Approval key 길이: {}", approvalKey != null ? approvalKey.length() : "null");
      }
      webSocketRef.set(null);
      throw new DomainException(WebSocketErrorCode.WEBSOCKET_CONNECTION_FAILED);
    } catch (Exception ex) {
      log.error("KIS WebSocket 연결 실패", ex);
      webSocketRef.set(null);
      throw new DomainException(WebSocketErrorCode.WEBSOCKET_CONNECTION_FAILED);
    } finally {
      connecting.set(false);
    }
  }

  private void closeConnection() {
    WebSocket webSocket = webSocketRef.getAndSet(null);
    if (webSocket != null) {
      log.info("KIS WebSocket 연결 종료");
      webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "no subscriptions");
    }
    approvalKey = null;
  }

  private void sendSubscription(String symbol, boolean subscribe) {
    WebSocket webSocket = webSocketRef.get();
    if (webSocket == null) {
      log.warn("WebSocket이 연결되지 않아 구독 요청을 보낼 수 없습니다. - symbol: {}", symbol);
      return;
    }

    String cachedApprovalKey = approvalKey;
    if (cachedApprovalKey == null) {
      log.error("Approval Key가 없어 구독 요청을 보낼 수 없습니다.");
      throw new DomainException(WebSocketErrorCode.UNAUTHORIZED);
    }

    Map<String, Object> header = new HashMap<>();
    header.put("approval_key", cachedApprovalKey);
    header.put("custtype", CUST_TYPE);
    header.put("tr_type", subscribe ? "1" : "0");
    header.put("content-type", "utf-8");

    Map<String, Object> input = new HashMap<>();
    input.put("tr_id", TR_ID);
    input.put("tr_key", symbol);

    Map<String, Object> body = new HashMap<>();
    body.put("input", input);

    Map<String, Object> payload = new HashMap<>();
    payload.put("header", header);
    payload.put("body", body);

    try {
      String json = objectMapper.writeValueAsString(payload);
      webSocket.sendText(json, true);
      log.debug("구독 요청 전송 - symbol: {}, type: {}", symbol, subscribe ? "구독" : "구독 해제");
    } catch (Exception ex) {
      log.error("구독 요청 전송 실패 - symbol: {}, type: {}", symbol, subscribe ? "구독" : "구독 해제", ex);
      throw new DomainException(WebSocketErrorCode.SUBSCRIPTION_FAILED);
    }
  }

    private class KisWebSocketListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

    @Override
    public void onOpen(WebSocket webSocket) {
      log.info("KIS WebSocket 연결 수립됨");
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      buffer.append(data);
      if (last) {
        String message = buffer.toString();
        buffer.setLength(0);
        log.trace("KIS WebSocket 메시지 수신 - length: {}", message.length());
        messageHandler.handleMessage(message, symbolToStockId::get);
      }
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      log.info("KIS WebSocket 연결 종료됨 - statusCode: {}, reason: {}", statusCode, reason);
      webSocketRef.set(null);
      approvalKey = null;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
      log.error("KIS WebSocket 에러 발생", error);
      webSocketRef.set(null);
      approvalKey = null;
    }
    }
}
