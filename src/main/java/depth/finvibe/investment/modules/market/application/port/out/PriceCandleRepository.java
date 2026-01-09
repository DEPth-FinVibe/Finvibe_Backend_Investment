package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;

import java.time.LocalDateTime;
import java.util.List;

public interface PriceCandleRepository {
    List<PriceCandle> findByStockIdAndTimeframeOrderByAtDesc(Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe);

    List<CurrentPrice> findCurrentPricesByStockIds(List<Long> stockIds);

}
