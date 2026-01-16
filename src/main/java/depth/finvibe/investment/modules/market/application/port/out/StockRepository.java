package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface StockRepository {

    Optional<Stock> findBySymbol(String symbol);

    void save(Stock stock);

    Page<Stock> findTop100ByOrderByCurrentValueDesc(Pageable pageable);

    Page<Stock> findTop100ByOrderByCurrentVolumeDesc(Pageable pageable);

    Page<Stock> findTop100ByOrderByPrevDayChangePctDesc(Pageable pageable);

    Page<Stock> findTop100ByOrderByPrevDayChangePctAsc(Pageable pageable);
}
