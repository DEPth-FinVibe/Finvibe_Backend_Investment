package depth.finvibe.investment.modules.market.api.external;

import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketQueryUseCase marketQueryUseCase;

    @GetMapping("/stocks/{stockId}/candles")
    public ResponseEntity<List<PriceCandleDto.Response>> getStockCandles(
            @PathVariable Long stockId,
            @RequestParam LocalDateTime startTime,
            @RequestParam Timeframe timeframe,
            @RequestParam(defaultValue = "100") Integer count
    ) {
        List<PriceCandleDto.Response> candles = marketQueryUseCase.getStockCandles(
                stockId, startTime, timeframe, count
        );
        return ResponseEntity.ok(candles);
    }

    @GetMapping("/stocks/current-prices")
    public ResponseEntity<List<CurrentPriceDto.Response>> getCurrentPrices(
            @RequestParam List<Long> stockIds
    ) {
        List<CurrentPriceDto.Response> currentPrices = marketQueryUseCase.getCurrentPrices(stockIds);
        return ResponseEntity.ok(currentPrices);
    }

    @GetMapping("/stocks/top-by-value")
    public ResponseEntity<List<StockDto.Response>> getTopStocksByValue() {
        List<StockDto.Response> stocks = marketQueryUseCase.getTopStocksByValue();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/stocks/top-by-volume")
    public ResponseEntity<List<StockDto.Response>> getTopStocksByVolume() {
        List<StockDto.Response> stocks = marketQueryUseCase.getTopStocksByVolume();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/stocks/top-rising")
    public ResponseEntity<List<StockDto.Response>> getTopRisingStocks() {
        List<StockDto.Response> stocks = marketQueryUseCase.getTopRisingStocks();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/stocks/top-falling")
    public ResponseEntity<List<StockDto.Response>> getTopFallingStocks() {
        List<StockDto.Response> stocks = marketQueryUseCase.getTopFallingStocks();
        return ResponseEntity.ok(stocks);
    }
}
