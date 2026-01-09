package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final PriceCandleRepository priceCandleRepository;
    private final StockRepository stockRepository;
    private final CurrentPriceRepository currentPriceRepository;

    public List<PriceCandle> getStockCandles(Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe) {
        return priceCandleRepository.findByStockIdAndTimeframeOrderByAtDesc(stockId, startTime, endTime, timeframe);
    }

    public List<CurrentPrice> getCurrentPrices(List<Long> stockIds) {
        List<CurrentPrice> prices = currentPriceRepository.findByStockIds(stockIds);

        // 캐시 미스 처리
        if (prices.size() < stockIds.size()) {
            List<Long> missedIds = findMissedStockIds(stockIds, prices);
            List<CurrentPrice> fallbackPrices = loadFromDatabase(missedIds);
            currentPriceRepository.saveAll(fallbackPrices);
            prices.addAll(fallbackPrices);
        }

        return prices;
    }

    // 거래대금 TOP100
    public Page<Stock> getTopStocksByValue(Pageable pageable) {
        return stockRepository.findTop100ByOrderByCurrentValueDesc(pageable);
    }

    // 거래량 TOP100
    public Page<Stock> getTopStocksByVolume(Pageable pageable) {
        return stockRepository.findTop100ByOrderByCurrentVolumeDesc(pageable);
    }

    // 급상승 TOP100
    public Page<Stock> getTopRisingStocks(Pageable pageable) {
        return stockRepository.findTop100ByOrderByPrevDayChangePctDesc(pageable);
    }

    // 급하락 TOP100
    public Page<Stock> getTopFallingStocks(Pageable pageable) {
        return stockRepository.findTop100ByOrderByPrevDayChangePctAsc(pageable);
    }

    // 캐시 갱신 (스케줄러/이벤트에서 호출)
    public void updateCurrentPrice(Long stockId) {
        PriceCandle latest = priceCandleRepository
                .findFirstByStockIdAndTimeframeOrderByAtDesc(stockId, Timeframe.DAY)
                .orElseThrow();

        CurrentPrice currentPrice = new CurrentPrice(
                latest.getStockId(),
                latest.getAt(),
                latest.getClose(),
                latest.getOpen(),
                latest.getHigh(),
                latest.getLow(),
                latest.getClose(),
                latest.getPrevDayChangePct(),
                latest.getVolume(),
                latest.getValue()
        );

        currentPriceRepository.save(currentPrice);
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
