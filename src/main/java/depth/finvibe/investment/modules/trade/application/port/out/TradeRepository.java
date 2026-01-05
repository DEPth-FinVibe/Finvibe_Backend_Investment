package depth.finvibe.investment.modules.trade.application.port.out;

import depth.finvibe.investment.modules.trade.domain.Trade;

import java.util.Optional;

public interface TradeRepository {

    Trade save(Trade trade);
    Optional<Trade> findById(Long tradeId);
}
