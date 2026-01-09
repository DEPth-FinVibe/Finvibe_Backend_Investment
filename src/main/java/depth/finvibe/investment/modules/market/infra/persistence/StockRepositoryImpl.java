package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class StockRepositoryImpl implements StockRepository {
    @Override
    public Page<Stock> findTop100ByOrderByCurrentValueDesc(Pageable pageable) {
        return null;
    }

    @Override
    public Page<Stock> findTop100ByOrderByCurrentVolumeDesc(Pageable pageable) {
        return null;
    }

    @Override
    public Page<Stock> findTop100ByOrderByPrevDayChangePctDesc(Pageable pageable) {
        return null;
    }

    @Override
    public Page<Stock> findTop100ByOrderByPrevDayChangePctAsc(Pageable pageable) {
        return null;
    }
}
