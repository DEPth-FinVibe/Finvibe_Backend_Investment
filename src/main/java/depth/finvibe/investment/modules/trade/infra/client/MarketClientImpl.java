package depth.finvibe.investment.modules.trade.infra.client;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.market.application.port.in.MarketQueryUseCase;
import depth.finvibe.investment.modules.market.application.port.in.MarketStatusQueryUseCase;
import depth.finvibe.investment.modules.market.domain.enums.MarketStatus;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.MarketStatusDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.trade.application.port.out.MarketClient;
import depth.finvibe.investment.modules.trade.domain.error.TradeErrorCode;
import depth.finvibe.investment.shared.error.DomainException;

@Component
@RequiredArgsConstructor
public class MarketClientImpl implements MarketClient {
    private final MarketStatusQueryUseCase marketStatusQueryUseCase;
    private final MarketQueryUseCase marketQueryUseCase;

    @Override
    public boolean isMarketOpen() {
        MarketStatusDto.Response status = marketStatusQueryUseCase.getMarketStatus();
        return status.getStatus().equals(MarketStatus.OPEN);
    }

    @Override
    public Long getCurrentPrice(Long stockId) {
        List<CurrentPriceDto.Response> prices = marketQueryUseCase.getCurrentPrices(List.of(stockId));
        if (!prices.isEmpty()) {
            return prices.getFirst().getClose().longValue();
        }

        LocalDateTime targetTime = Timeframe.MINUTE.lastCompletedTime(LocalDateTime.now());
        List<PriceCandleDto.Response> candles = marketQueryUseCase.getStockCandles(
                stockId,
                targetTime,
                targetTime,
                Timeframe.MINUTE
        );
        if (candles.isEmpty()) {
            throw new DomainException(TradeErrorCode.MARKET_PRICE_MISMATCH);
        }

        return candles.getFirst().getClose().longValue();
    }
}
