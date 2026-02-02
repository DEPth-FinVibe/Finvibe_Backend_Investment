package depth.finvibe.investment.modules.trade.infra.client;

import depth.finvibe.investment.modules.market.application.port.in.MarketStatusQueryUseCase;
import depth.finvibe.investment.modules.market.domain.enums.MarketStatus;
import depth.finvibe.investment.modules.market.dto.MarketStatusDto;
import depth.finvibe.investment.modules.trade.application.port.out.MarketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketClientImpl implements MarketClient {
    private final MarketStatusQueryUseCase marketStatusQueryUseCase;

    @Override
    public boolean isMarketOpen() {
        MarketStatusDto.Response status = marketStatusQueryUseCase.getMarketStatus();
        return status.getStatus().equals(MarketStatus.OPEN);
    }
}
