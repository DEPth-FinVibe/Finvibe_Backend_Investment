package depth.finvibe.investment.modules.market.infra.client;

import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RealMarketClientImpl implements RealMarketClient {
    @Override
    public List<StockDto.TopStockResponse> getTopStocksByValue(int limit) {
        return List.of();
    }

    @Override
    public List<StockDto.TopStockResponse> getTopStocksByVolume(int limit) {
        return List.of();
    }

    @Override
    public List<StockDto.TopStockResponse> getTopRisingStocks(int limit) {
        return List.of();
    }

    @Override
    public List<StockDto.TopStockResponse> getTopFallingStocks(int limit) {
        return List.of();
    }

    @Override
    public List<PriceCandleDto.Response> fetchPriceCandles(Long stockId, List<LocalDateTime> missingCandleTimes, Timeframe timeframe) {
        return List.of();
    }
}
