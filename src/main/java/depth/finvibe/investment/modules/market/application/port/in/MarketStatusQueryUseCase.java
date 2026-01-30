package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.modules.market.dto.MarketStatusDto;

public interface MarketStatusQueryUseCase {

  MarketStatusDto.Response getMarketStatus();
}
