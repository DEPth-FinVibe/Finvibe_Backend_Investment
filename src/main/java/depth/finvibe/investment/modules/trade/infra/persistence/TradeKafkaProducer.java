package depth.finvibe.investment.modules.trade.infra.persistence;

import depth.finvibe.investment.modules.trade.application.port.out.TradeEventProducer;
import depth.finvibe.investment.modules.trade.domain.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeKafkaProducer implements TradeEventProducer {

    @Override
    public void publishNormalTradeExecutedEvent(Trade trade) {

    }

    @Override
    public void publishReservedTradeExecutedEvent(Trade trade) {

    }
}
