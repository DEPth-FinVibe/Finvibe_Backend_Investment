package depth.finvibe.investment.modules.market.application.port.out;

import java.util.UUID;

public interface HoldingStockRepository {
    void registerHoldingStock(Long stockId, UUID userId);
    void unregisterHoldingStock(Long stockId, UUID userId);
}
