package depth.finvibe.investment.modules.trade.infra.messaging;

import depth.finvibe.investment.modules.trade.application.port.out.TradeEventProducer;
import depth.finvibe.investment.modules.trade.domain.Trade;
import depth.finvibe.investment.modules.trade.domain.enums.MarketType;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
        return TradeExecutedEvent.builder()
                .tradeId(trade.getId().toString())
                .userId(trade.getUserId().toString())
                .type(trade.getTransactionType().name())
                .amount(BigDecimal.valueOf(trade.getAmount()))
                .price(BigDecimal.valueOf(trade.getPrice()))
                .stockId(trade.getStockId())
                .name("Unknown") //TODO: 종목명도 Trade 엔티티에서 저장하도록 수정
                .currency(trade.getMarketType() == MarketType.DOMESTIC ? "KRW" : "USD")
                .portfolioId(trade.getPortfolioId())
                .build();
    }
}
