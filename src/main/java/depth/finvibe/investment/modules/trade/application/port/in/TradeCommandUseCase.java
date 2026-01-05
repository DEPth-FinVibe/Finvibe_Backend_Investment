package depth.finvibe.investment.modules.trade.application.port.in;

import depth.finvibe.investment.modules.trade.dto.TradeDto;

public interface TradeCommandUseCase {

    TradeDto.TradeResponse createTrade(TradeDto.TransactionRequest request);

    TradeDto.TradeResponse cancelTrade(Long tradeId);

    TradeDto.TradeResponse executeReservedTrade(Long tradeId);
}

