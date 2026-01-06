package depth.finvibe.investment.modules.trade.infra.messaging;

import depth.finvibe.investment.modules.trade.application.port.out.TradeEventProducer;
import depth.finvibe.investment.modules.trade.domain.Trade;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeKafkaProducer implements TradeEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TRADE_EXECUTED_TOPIC = "trade.trade-executed.v1";

    @Override
    public void publishNormalTradeExecutedEvent(Trade trade) {
        log.info("Publishing normal trade executed event for trade: {}", trade.getId());
        TradeExecutedEvent event = createTradeExecutedEvent(trade);
        kafkaTemplate.send(TRADE_EXECUTED_TOPIC, trade.getUserId().toString(), event);
    }

    @Override
    public void publishReservedTradeExecutedEvent(Trade trade) {
        log.info("Publishing reserved trade executed event for trade: {}", trade.getId());
        TradeExecutedEvent event = createTradeExecutedEvent(trade);
        kafkaTemplate.send(TRADE_EXECUTED_TOPIC, trade.getUserId().toString(), event);
    }

    private TradeExecutedEvent createTradeExecutedEvent(Trade trade) {
        return new TradeExecutedEvent(
                trade.getId().toString(),
                trade.getUserId().toString(),
                trade.getTransactionType().name(),
                trade.getAmount(),
                trade.getPrice()
        );
    }
}
