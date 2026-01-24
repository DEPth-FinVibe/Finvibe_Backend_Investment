package depth.finvibe.investment.modules.market.api.external;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import depth.finvibe.investment.modules.market.domain.enums.MarketSearchType;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import depth.finvibe.investment.shared.error.DomainException;
@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketQueryUseCase marketQueryUseCase;

    @GetMapping("/stocks/{stockId}/candles")
    public ResponseEntity<List<PriceCandleDto.Response>> getStockCandles(
            @PathVariable Long stockId,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime,
            @RequestParam Timeframe timeframe
    ) {
        // 시작 시각이 종료 시각보다 이후인지 검증
        if (startTime.isAfter(endTime)) {
            throw new DomainException(MarketErrorCode.INVALID_START_END_TIME);
        }
        
        // 종료 시각이 완료된 캔들 범위 내에 있는지 검증
        LocalDateTime lastCompletedCandleTime = getLastCompletedCandleTime(timeframe);
        if (endTime.isAfter(lastCompletedCandleTime)) {
            throw new DomainException(MarketErrorCode.INVALID_TIME_RANGE);
        }
        
        List<PriceCandleDto.Response> candles = marketQueryUseCase.getStockCandles(
                stockId, startTime, endTime, timeframe
        );
        return ResponseEntity.ok(candles);
    }
    
    /**
     * Timeframe별로 현재 시점에서 완료된 마지막 캔들의 시각을 계산
     */
    private LocalDateTime getLastCompletedCandleTime(Timeframe timeframe) {
        LocalDateTime now = LocalDateTime.now();
        return timeframe.lastCompletedTime(now);
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

    @GetMapping("/stocks/search")
    public ResponseEntity<List<StockDto.Response>> searchStocks(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "ALL") MarketSearchType marketType
    ) {
        List<StockDto.Response> stocks = marketQueryUseCase.searchStocks(query, marketType);
        return ResponseEntity.ok(stocks);
    }
}
