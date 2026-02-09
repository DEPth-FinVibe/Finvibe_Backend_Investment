package depth.finvibe.investment.modules.trade.application.port.out;

import depth.finvibe.investment.modules.trade.domain.Trade;

public interface TradeEventProducer {

    void publishNormalTradeExecutedEvent(Trade trade);
    void publishTradeReservedEvent(Trade trade);
    void publishTradeCancelledEvent(Trade trade);
}
