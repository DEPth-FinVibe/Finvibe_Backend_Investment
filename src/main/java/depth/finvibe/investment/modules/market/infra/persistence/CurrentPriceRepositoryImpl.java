package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CurrentPriceRepositoryImpl implements CurrentPriceRepository {
    @Override
    public void save(CurrentPrice currentPrice) {

    }

    @Override
    public void saveAll(List<CurrentPrice> currentPrices) {

    }

    @Override
    public Optional<CurrentPrice> findByStockId(Long stockId) {
        return Optional.empty();
    }

    @Override
    public List<CurrentPrice> findByStockIds(List<Long> stockIds) {
        return List.of();
    }

    @Override
    public void deleteByStockId(Long stockId) {

    }

    @Override
    public List<Long> getTopStockIdsByValue(int limit) {
        return List.of();
    }

    @Override
    public List<Long> getTopStockIdsByVolume(int limit) {
        return List.of();
    }

    @Override
    public List<Long> getTopRisingStockIds(int limit) {
        return List.of();
    }

    @Override
    public List<Long> getTopFallingStockIds(int limit) {
        return List.of();
    }
}
