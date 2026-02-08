package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;

import java.time.LocalDateTime;
import java.util.List;

public interface PriceCandleRepository {

    List<PriceCandle> findExisting(Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe);

    List<PriceCandle> findByStockIdAndTimeframeAndAtIn(Long stockId, Timeframe timeframe, List<LocalDateTime> times);

    List<PriceCandle> findLatestByStockIdsAndTimeframe(List<Long> stockIds, Timeframe timeframe);

    void saveAll(List<PriceCandle> fetchedResult);

    /**
     * 특정 Stock과 Timeframe으로 데이터 존재 여부 확인
     */
    boolean existsByStockIdAndTimeframe(Long stockId, Timeframe timeframe);
}
