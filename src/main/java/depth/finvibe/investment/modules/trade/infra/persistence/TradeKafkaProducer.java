package depth.finvibe.investment.modules.trade.infra.persistence;

import depth.finvibe.investment.modules.trade.application.port.out.TradeEventProducer;
import depth.finvibe.investment.modules.trade.domain.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeKafkaProducer implements TradeEventProducer {

    @Override
    public void publishNormalTradeExecutedEvent(Trade trade) {
    }

    @Override
    public void publishReservedTradeExecutedEvent(Trade trade) {

    }
}
