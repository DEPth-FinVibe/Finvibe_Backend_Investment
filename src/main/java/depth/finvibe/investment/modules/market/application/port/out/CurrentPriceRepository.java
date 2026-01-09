package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import java.util.List;
import java.util.Optional;

public interface CurrentPriceRepository {
    void save(CurrentPrice currentPrice);
    void saveAll(List<CurrentPrice> currentPrices);
    Optional<CurrentPrice> findByStockId(Long stockId);
    List<CurrentPrice> findByStockIds(List<Long> stockIds);
    void deleteByStockId(Long stockId);

    // Sorted Set 기반 TOP100 조회
    List<Long> getTopStockIdsByValue(int limit);
    List<Long> getTopStockIdsByVolume(int limit);
    List<Long> getTopRisingStockIds(int limit);
    List<Long> getTopFallingStockIds(int limit);
}