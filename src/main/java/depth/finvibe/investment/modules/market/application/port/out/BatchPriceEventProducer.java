package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.shared.dto.BatchPriceUpdatedEvent;

public interface BatchPriceEventProducer {
    void publishBatchPriceUpdated(BatchPriceUpdatedEvent event);
}
