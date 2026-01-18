package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketQueryUseCase {

    List<PriceCandleDto.Response> getStockCandles(
            Long stockId,
            LocalDateTime startTime,
            Timeframe timeframe,
            Integer count
    );

    List<CurrentPriceDto.Response> getCurrentPrices(List<Long> stockIds);

    List<StockDto.Response> getTopStocksByValue(Pageable pageable);

    List<StockDto.Response> getTopStocksByVolume(Pageable pageable);

    List<StockDto.Response> getTopRisingStocks(Pageable pageable);

    List<StockDto.Response> getTopFallingStocks(Pageable pageable);
}