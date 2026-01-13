package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.MarketCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.investment.modules.market.application.port.out.PriceUpdatePublisher;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.PriceCandle;

import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService implements MarketQueryUseCase, MarketCommandUseCase {

    private final PriceCandleRepository priceCandleRepository;
    private final StockRepository stockRepository;
    private final CurrentPriceRepository currentPriceRepository;
    private final PriceUpdatePublisher priceUpdatePublisher;

    @Override
    public List<PriceCandleDto.Response> getStockCandles(Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe) {
        return priceCandleRepository
                .findByStockIdAndTimeframeOrderByAtDesc(stockId, startTime, endTime, timeframe)
                .stream()
                .map(candle -> PriceCandleDto.Response.from(
                        candle.getStockId(),
                        candle.getOpen(),
                        candle.getClose(),
                        candle.getHigh(),
                        candle.getLow(),
                        candle.getVolume(),
                        candle.getValue(),
                        candle.getTimeframe(),
                        candle.getAt(),
                        candle.getPrevDayChangePct()
                ))
                .toList();
    }

    @Override
    public List<CurrentPriceDto.Response> getCurrentPrices(List<Long> stockIds) {
        List<CurrentPrice> prices = currentPriceRepository.findByStockIds(stockIds);

        /**
         * 캐시 미스 처리
         * - 요청한 주식 ID 중에서 현재가가 없는 경우, 데이터베이스에서 최신 가격 정보를 로드하여 현재가 테이블에 저장
         *  cache aside pattern
         */
        if (prices.size() < stockIds.size()) {
            List<Long> missedIds = findMissedStockIds(stockIds, prices);
            List<CurrentPrice> fallbackPrices = loadFromDatabase(missedIds);
            currentPriceRepository.saveAll(fallbackPrices);
            prices.addAll(fallbackPrices);
        }

        return prices.stream()
                .map(price -> CurrentPriceDto.Response.from(
                        price.stockId(),
                        Timeframe.DAY,
                        price.at(),
                        price.open(),
                        price.high(),
                        price.low(),
                        price.close(),
                        price.prevDayChangePct(),
                        price.volume(),
                        price.value()
                ))
                .toList();
    }

    // 거래대금 TOP100
    public Page<StockDto.Response> getTopStocksByValue(Pageable pageable) {
        return stockRepository.findTop100ByOrderByCurrentValueDesc(pageable)
                .map(stock -> new StockDto.Response(
                        stock.getId(),
                        stock.getName(),
                        stock.getName(),
                        stock.getCategoryId()
                ));
    }

    // 거래량 TOP100
    public Page<StockDto.Response> getTopStocksByVolume(Pageable pageable) {

        return stockRepository.findTop100ByOrderByCurrentVolumeDesc(pageable)
                .map(stock -> new StockDto.Response(
                        stock.getId(),
                        stock.getName(),
                        stock.getName(),
                        stock.getCategoryId()
                ));
    }

    // 급상승 TOP100
    public Page<StockDto.Response> getTopRisingStocks(Pageable pageable) {
        return stockRepository.findTop100ByOrderByPrevDayChangePctDesc(pageable)
                .map(stock -> new StockDto.Response(
                        stock.getId(),
                        stock.getName(),
                        stock.getName(),
                        stock.getCategoryId()
                ));
    }

    // 급하락 TOP100
    public Page<StockDto.Response> getTopFallingStocks(Pageable pageable) {
        return stockRepository.findTop100ByOrderByPrevDayChangePctAsc(pageable)
                .map(stock -> new StockDto.Response(
                        stock.getId(),
                        stock.getName(),
                        stock.getName(),
                        stock.getCategoryId()
                ));
    }

    @Transactional
    public void updateCurrentPrices(List<Long> stockIds) {
        List<PriceCandle> latestCandles = priceCandleRepository
                .findLatestForEachStock(stockIds, Timeframe.DAY);

        List<CurrentPrice> currentPrices = latestCandles.stream()
                .map(this::convertToCurrentPrice)
                .toList();

        currentPriceRepository.saveAll(currentPrices);

        // WebSocket으로 일괄 발행
        List<CurrentPriceDto.Response> priceUpdates = currentPrices.stream()
                .map(price -> CurrentPriceDto.Response.from(
                        price.stockId(),
                        Timeframe.DAY,
                        price.at(),
                        price.open(),
                        price.high(),
                        price.low(),
                        price.close(),
                        price.prevDayChangePct(),
                        price.volume(),
                        price.value()
                ))
                .toList();

        try {
            priceUpdatePublisher.publishBulkPriceUpdate(priceUpdates);
            log.debug("Published bulk price updates for {} stocks", stockIds.size());
        } catch (Exception e) {
            log.error("Failed to publish bulk price updates: {}", e.getMessage());
        }
    }

    private List<Long> findMissedStockIds(List<Long> requested, List<CurrentPrice> found) {
        List<Long> foundIds = found.stream()
                .map(CurrentPrice::stockId)
                .toList();
        return requested.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
    }

    private List<CurrentPrice> loadFromDatabase(List<Long> stockIds) {
        return priceCandleRepository
                .findLatestForEachStock(stockIds, Timeframe.DAY)
                .stream()
                .map(this::convertToCurrentPrice)
                .toList();
    }

    private CurrentPrice convertToCurrentPrice(PriceCandle candle) {
        return new CurrentPrice(
                candle.getStockId(),
                candle.getAt(),
                candle.getClose(),
                candle.getOpen(),
                candle.getHigh(),
                candle.getLow(),
                candle.getClose(),
                candle.getPrevDayChangePct(),
                candle.getVolume(),
                candle.getValue()
        );
    }
}
