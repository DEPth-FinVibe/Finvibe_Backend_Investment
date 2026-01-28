package depth.finvibe.investment.modules.asset.application.port.in;

import depth.finvibe.investment.shared.dto.BatchPriceUpdatedEvent;
import depth.finvibe.investment.shared.dto.SignUpEvent;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;

public interface AssetEventUseCase {
    void handleTradeExecutedEvent(TradeExecutedEvent event);

    void handleSignUpEvent(SignUpEvent event);

    void handleBatchPriceUpdatedEvent(BatchPriceUpdatedEvent event);
}
