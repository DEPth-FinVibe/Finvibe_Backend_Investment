package depth.finvibe.investment.modules.trade.application.port.in;

import depth.finvibe.investment.modules.trade.dto.TradeDto;

import java.util.List;
import java.util.UUID;

public interface TradeQueryUseCase {
    TradeDto.TradeResponse findTrade(Long tradeId);

    List<Long> findReservedStockIds(UUID userId);
}
