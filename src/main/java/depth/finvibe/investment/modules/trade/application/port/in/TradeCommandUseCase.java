package depth.finvibe.investment.modules.trade.application.port.in;

import depth.finvibe.investment.boot.security.model.Requester;
import depth.finvibe.investment.modules.trade.dto.TradeDto;

public interface TradeCommandUseCase {

    TradeDto.TradeResponse createTrade(TradeDto.TransactionRequest request, Requester requester);

    TradeDto.TradeResponse cancelTrade(Long tradeId, Requester requester);

    TradeDto.TradeResponse executeReservedTrade(Long tradeId);
}

