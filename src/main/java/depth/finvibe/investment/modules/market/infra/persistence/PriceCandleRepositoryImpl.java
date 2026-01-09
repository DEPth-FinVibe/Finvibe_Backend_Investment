package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PriceCandleRepositoryImpl implements PriceCandleRepository {
    @Override
    public List<PriceCandle> findByStockIdAndTimeframeOrderByAtDesc(Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe) {
        return List.of();
    }

    @Override
    public Optional<PriceCandle> findFirstByStockIdAndTimeframeOrderByAtDesc(Long stockId, Timeframe timeframe) {
        return Optional.empty();
    }

    @Override
    public List<PriceCandle> findLatestForEachStock(List<Long> stockIds, Timeframe timeframe) {
        return List.of();
    }
}
