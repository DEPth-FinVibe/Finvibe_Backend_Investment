package depth.finvibe.investment.modules.trade.infra.persistenc;

import depth.finvibe.investment.modules.trade.application.port.out.TradeRepository;
import depth.finvibe.investment.modules.trade.domain.Trade;

import java.util.Optional;

public class TradeRepositoryImpl implements TradeRepository {

    @Override
    public Trade save(Trade trade) {
        return null;
    }

    @Override
    public Optional<Trade> findById(Long tradeId) {
        return Optional.empty();
    }
}
