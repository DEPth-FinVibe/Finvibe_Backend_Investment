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

    @Value("${market.kis.websocket.url:wss://openapi.koreainvestment.com:9443/websocket}")
    private String websocketUrl;

    public synchronized void syncSubscriptions(List<Long> stockIds) {
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

        if (!toSubscribe.isEmpty() || !toUnsubscribe.isEmpty()) {
            ensureConnected();
        }

        for (String symbol : toSubscribe) {
            sendSubscription(symbol, true);
            subscribedSymbols.add(symbol);
        }

        for (String symbol : toUnsubscribe) {
            sendSubscription(symbol, false);
            subscribedSymbols.remove(symbol);
        }

        symbolToStockId.clear();
        symbolToStockId.putAll(latestMap);

        if (subscribedSymbols.isEmpty()) {
            closeConnection();
        }
    }

    private void ensureConnected() {
        if (webSocketRef.get() != null || !connecting.compareAndSet(false, true)) {
            return;
        }
        try {
            String approvalKey = approvalClient.requestApprovalKey();
            if (approvalKey == null) {
                throw new DomainException(WebSocketErrorCode.UNAUTHORIZED);
            }
            this.approvalKey = approvalKey;
            WebSocket webSocket = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(URI.create(websocketUrl), new KisWebSocketListener())
                    .join();
            webSocketRef.set(webSocket);
        } catch (Exception ex) {
            webSocketRef.set(null);
            throw new DomainException(WebSocketErrorCode.WEBSOCKET_CONNECTION_FAILED);
        } finally {
            connecting.set(false);
        }
    }

    private void closeConnection() {
        WebSocket webSocket = webSocketRef.getAndSet(null);
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "no subscriptions");
        }
        approvalKey = null;
    }

    private void sendSubscription(String symbol, boolean subscribe) {
        WebSocket webSocket = webSocketRef.get();
        if (webSocket == null) {
            return;
        }

        String cachedApprovalKey = approvalKey;
        if (cachedApprovalKey == null) {
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
        } catch (Exception ex) {
            log.warn("Failed to send websocket subscription for {}", symbol, ex);
            throw new DomainException(WebSocketErrorCode.SUBSCRIPTION_FAILED);
        }
    }

    private class KisWebSocketListener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                messageHandler.handleMessage(message, symbolToStockId::get);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            webSocketRef.set(null);
            approvalKey = null;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            webSocketRef.set(null);
            approvalKey = null;
            log.warn("Websocket error: {}", error.getMessage());
        }
    }
}
