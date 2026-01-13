package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;

import java.util.List;

public interface MarketCommandUseCase {

    void updateCurrentPrices(List<Long> stockIds);

}