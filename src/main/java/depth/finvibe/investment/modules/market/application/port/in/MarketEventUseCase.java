package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.shared.dto.StockHoldingChangedEvent;

public interface MarketEventUseCase {
    void handleStockHoldingChangedEvent(StockHoldingChangedEvent event);
}
