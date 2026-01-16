package depth.finvibe.investment.modules.market.domain;

import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;

public interface StockObserver {
    ManagingStockGroup getManagingStockGroup();
    void onPriceUpdate(CurrentPriceDto.Response priceUpdate);
}
