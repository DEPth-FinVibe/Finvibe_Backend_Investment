package depth.finvibe.investment.modules.market.api.external;

import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import depth.finvibe.investment.shared.error.DomainException;
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
        
        return switch (timeframe) {
            case MINUTE -> 
                // 현재 분은 아직 진행중이므로 이전 분
                now.minusMinutes(1).withSecond(0).withNano(0);
                
            case HOUR -> 
                // 현재 시간은 아직 진행중이므로 이전 시간
                now.minusHours(1).withMinute(0).withSecond(0).withNano(0);
                
            case DAY -> 
                // 오늘은 아직 진행중이므로 어제
                now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                
            case WEEK -> {
                // 이번 주는 아직 진행중이므로 지난 주 월요일
                LocalDateTime lastWeekMonday = now.minusWeeks(1)
                        .with(java.time.DayOfWeek.MONDAY)
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                yield lastWeekMonday;
            }
            
            case MONTH -> {
                // 이번 달은 아직 진행중이므로 지난 달 1일
                LocalDateTime lastMonthFirst = now.minusMonths(1)
                        .withDayOfMonth(1)
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                yield lastMonthFirst;
            }
            
            case YEAR -> {
                // 올해는 아직 진행중이므로 작년 1월 1일
                LocalDateTime lastYearFirst = now.minusYears(1)
                        .withMonth(1)
                        .withDayOfMonth(1)
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                yield lastYearFirst;
            }
        };
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
