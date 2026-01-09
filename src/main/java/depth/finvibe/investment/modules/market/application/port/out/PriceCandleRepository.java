package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PriceCandleRepository {
    List<PriceCandle> findByStockIdAndTimeframeOrderByAtDesc(Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe);

    Optional<PriceCandle> findFirstByStockIdAndTimeframeOrderByAtDesc(Long stockId, Timeframe timeframe);

    List<PriceCandle> findLatestForEachStock(List<Long> stockIds, Timeframe timeframe);
}
