package depth.finvibe.investment.modules.trade.application.port.out;

import depth.finvibe.investment.modules.trade.domain.Trade;

public interface TradeProducer {

    void publishTradeExecutedEvent(Trade trade);
}
