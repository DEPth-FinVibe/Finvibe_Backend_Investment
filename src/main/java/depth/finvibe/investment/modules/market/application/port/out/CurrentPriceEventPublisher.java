package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.dto.CurrentPriceUpdatedEvent;

public interface CurrentPriceEventPublisher {
    void publish(CurrentPriceUpdatedEvent event);
}
