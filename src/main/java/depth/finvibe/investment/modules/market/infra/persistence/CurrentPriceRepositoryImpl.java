package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CurrentPriceRepositoryImpl implements CurrentPriceRepository {

    @Override
    public void upsertCurrentPrice(CurrentPrice currentPrice) {

    }

    @Override
    public void deleteCurrentPrice(Long stockId) {

    }

    @Override
    public List<CurrentPrice> findByStockIds(List<Long> stockIds) {
        return List.of();
    }
}
