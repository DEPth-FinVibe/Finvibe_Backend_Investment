package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;

import java.time.LocalDateTime;
import java.util.List;

public interface MarketQueryUseCase {

    List<PriceCandleDto.Response> getStockCandles(
            Long stockId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Timeframe timeframe
    );

    List<CurrentPriceDto.Response> getCurrentPrices(List<Long> stockIds);

    List<StockDto.Response> getTopStocksByValue();

    List<StockDto.Response> getTopStocksByVolume();

    List<StockDto.Response> getTopRisingStocks();

    List<StockDto.Response> getTopFallingStocks();
}