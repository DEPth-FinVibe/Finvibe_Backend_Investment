package depth.finvibe.investment.modules.asset.application.port.in;

import java.util.List;

public interface ProfitCalculationUseCase {
    void recalculateAllProfits(List<Long> updatedStockIds);
}
