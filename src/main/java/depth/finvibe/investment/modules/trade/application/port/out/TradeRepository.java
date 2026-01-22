package depth.finvibe.investment.modules.trade.application.port.out;

import depth.finvibe.investment.modules.trade.domain.Trade;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradeRepository {

    Trade save(Trade trade);
    Optional<Trade> findById(Long tradeId);

    List<Long> findDistinctStockIdsByUserIdAndTradeType(UUID userId, TradeType tradeType);
}
