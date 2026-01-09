package depth.finvibe.investment.modules.market.application;

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

    public List<PriceCandle> getStockCandles(Long stockId, LocalDateTime startTime, LocalDateTime endTime, Timeframe timeframe) {
        return priceCandleRepository.findByStockIdAndTimeframeOrderByAtDesc(stockId, startTime, endTime, timeframe);
    }

    public List<CurrentPrice> getCurrentPrices(List<Long> stockIds) {
        return priceCandleRepository.findCurrentPricesByStockIds(stockIds);
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
}
