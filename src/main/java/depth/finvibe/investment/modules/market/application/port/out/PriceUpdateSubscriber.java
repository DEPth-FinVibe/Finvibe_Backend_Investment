package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.StockObserver;

/**
 * 외부 주식정보를 구독/관리하는 인터페이스
 */
public interface PriceUpdateSubscriber {

    void subscribe(StockObserver observer);

    void unsubscribe(Long stockId);

    void unsubscribeAll();
}
