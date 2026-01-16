package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.StockObserver;

public interface PriceUpdateSubscriber {

    void subscribe(StockObserver observer);

    void unsubscribe(Long stockId);

    void unsubscribeAll();
}
