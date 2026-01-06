package depth.finvibe.investment.modules.trade.application.port.in;

import depth.finvibe.investment.modules.trade.dto.TradeDto;

public interface TradeQueryUseCase {
    TradeDto.TradeResponse findTrade(Long tradeId);
}
