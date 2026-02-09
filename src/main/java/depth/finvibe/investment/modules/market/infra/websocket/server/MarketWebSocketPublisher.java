package depth.finvibe.investment.modules.market.infra.websocket.server;

import depth.finvibe.investment.modules.market.dto.CurrentPriceUpdatedEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketWebSocketPublisher {
    private static final String EXCHANGE = "KRX";

    private final MarketWebSocketRegistry registry;
    private final ObjectMapper objectMapper;

    public void publish(CurrentPriceUpdatedEvent event) {
        if (event == null || event.getStockId() == null) {
            return;
        }
        String topic = "quote:" + event.getStockId();
        Map<String, Object> payload = buildEventPayload(event, topic);
        String message;
        try {
            message = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Failed to serialize websocket event.", ex);
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        for (MarketWebSocketConnection connection : registry.getSubscribers(topic)) {
            try {
                if (connection.getSession().isOpen()) {
                    connection.getSession().sendMessage(textMessage);
                }
            } catch (Exception ex) {
                log.warn("Failed to send websocket event to session {}", connection.getSession().getId());
            }
        }
    }

    private Map<String, Object> buildEventPayload(CurrentPriceUpdatedEvent event, String topic) {
        Map<String, Object> data = new HashMap<>();
        data.put("stockId", event.getStockId());
        data.put("exchange", EXCHANGE);
        data.put("price", event.getClose());
        data.put("open", event.getOpen());
        data.put("high", event.getHigh());
        data.put("low", event.getLow());
        data.put("prevDayChangePct", event.getPrevDayChangePct());
        data.put("volume", event.getVolume());
        data.put("value", event.getValue());

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "event");
        payload.put("topic", topic);
        payload.put("ts", Instant.now().toEpochMilli());
        payload.put("data", data);
        return payload;
    }
}
