package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.CurrentPrice;

import java.util.List;

public interface CurrentPriceRepository {
    void upsertCurrentPrice(CurrentPrice currentPrice);
    void deleteCurrentPrice(Long stockId);

    List<CurrentPrice> findByStockIds(List<Long> stockIds);
}
