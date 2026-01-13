package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;

import java.util.List;

public interface MarketCommandUseCase {

    void updateCurrentPrices(List<Long> stockIds);

    void addRegionOfInterestLevel1(List<Long> interestStockIds);

    void addRegionOfInterestLevel2(List<Long> ownedStockIds);

    void removeRegionOfInterestLevel1(List<Long> interestStockIds);

    void removeRegionOfInterestLevel2(List<Long> ownedStockIds);


}