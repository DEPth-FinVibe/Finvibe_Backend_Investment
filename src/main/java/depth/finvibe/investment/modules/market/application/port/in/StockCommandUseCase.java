package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.modules.market.dto.StockDto;

import java.util.List;

public interface StockCommandUseCase {
    void bulkUpsertStocks();

    void renewStockCharts();
}
