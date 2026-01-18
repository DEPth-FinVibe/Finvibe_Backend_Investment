package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;

import java.time.LocalDateTime;
import java.util.List;

public interface PriceCandleRepository {

    List<PriceCandle> findExisting(Long stockId, LocalDateTime startTime, Timeframe timeframe, Integer count);

    void saveAll(List<PriceCandle> fetchedResult);
}
