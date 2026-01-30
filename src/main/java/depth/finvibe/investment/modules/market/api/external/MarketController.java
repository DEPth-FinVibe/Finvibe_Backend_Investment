package depth.finvibe.investment.modules.market.api.external;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import depth.finvibe.investment.modules.market.application.port.in.MarketStatusQueryUseCase;
import depth.finvibe.investment.modules.market.domain.enums.MarketSearchType;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.modules.market.dto.ClosingPriceDto;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.MarketStatusDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import depth.finvibe.investment.shared.error.DomainException;
@RestController
@RequiredArgsConstructor
@Tag(name = "시장", description = "시장 API")
public class MarketController {

    private final MarketQueryUseCase marketQueryUseCase;
    private final MarketStatusQueryUseCase marketStatusQueryUseCase;

    @GetMapping("/stocks/{stockId}/candles")
    @Operation(summary = "종목 캔들 조회", description = "종목의 캔들 데이터를 조회합니다.")
    public ResponseEntity<List<PriceCandleDto.Response>> getStockCandles(
            @Parameter(description = "종목 ID", example = "1") @PathVariable Long stockId,
            @Parameter(description = "시작 시각", example = "2024-01-01T09:00:00") @RequestParam LocalDateTime startTime,
            @Parameter(description = "종료 시각", example = "2024-01-02T15:30:00") @RequestParam LocalDateTime endTime,
            @Parameter(description = "타임프레임", example = "DAY") @RequestParam Timeframe timeframe
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
    @Operation(summary = "현재가 조회", description = "종목 현재가를 조회합니다.")
    public ResponseEntity<List<CurrentPriceDto.Response>> getCurrentPrices(
            @Parameter(description = "종목 ID 목록", example = "1,2,3") @RequestParam List<Long> stockIds
    ) {
        List<CurrentPriceDto.Response> currentPrices = marketQueryUseCase.getCurrentPrices(stockIds);
        return ResponseEntity.ok(currentPrices);
    }

    @GetMapping("/stocks/closing-prices")
    @Operation(summary = "종가 조회", description = "종목의 최신 종가를 조회합니다.")
    public ResponseEntity<List<ClosingPriceDto.Response>> getClosingPrices(
            @Parameter(description = "종목 ID 목록", example = "1,2,3") @RequestParam List<Long> stockIds
    ) {
        List<ClosingPriceDto.Response> closingPrices = marketQueryUseCase.getClosingPrices(stockIds);
        return ResponseEntity.ok(closingPrices);
    }

    @GetMapping("/stocks/top-by-value")
    @Operation(summary = "거래대금 TOP 조회", description = "거래대금 상위 종목을 조회합니다.")
    public ResponseEntity<List<StockDto.Response>> getTopStocksByValue() {
        List<StockDto.Response> stocks = marketQueryUseCase.getTopStocksByValue();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/stocks/top-by-volume")
    @Operation(summary = "거래량 TOP 조회", description = "거래량 상위 종목을 조회합니다.")
    public ResponseEntity<List<StockDto.Response>> getTopStocksByVolume() {
        List<StockDto.Response> stocks = marketQueryUseCase.getTopStocksByVolume();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/stocks/top-rising")
    @Operation(summary = "상승 TOP 조회", description = "상승률 상위 종목을 조회합니다.")
    public ResponseEntity<List<StockDto.Response>> getTopRisingStocks() {
        List<StockDto.Response> stocks = marketQueryUseCase.getTopRisingStocks();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/stocks/top-falling")
    @Operation(summary = "하락 TOP 조회", description = "하락률 상위 종목을 조회합니다.")
    public ResponseEntity<List<StockDto.Response>> getTopFallingStocks() {
        List<StockDto.Response> stocks = marketQueryUseCase.getTopFallingStocks();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/stocks/search")
    @Operation(summary = "종목 검색", description = "검색어와 시장 구분으로 종목을 조회합니다.")
    public ResponseEntity<List<StockDto.Response>> searchStocks(
            @Parameter(description = "검색어", example = "삼성") @RequestParam(defaultValue = "") String query,
            @Parameter(description = "시장 구분", example = "ALL") @RequestParam(defaultValue = "ALL") MarketSearchType marketType
    ) {
        List<StockDto.Response> stocks = marketQueryUseCase.searchStocks(query, marketType);
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/status")
    @Operation(summary = "장 상태 조회", description = "시장 장 상태를 조회합니다.")
    public ResponseEntity<MarketStatusDto.Response> getMarketStatus() {
        return ResponseEntity.ok(marketStatusQueryUseCase.getMarketStatus());
    }
}
