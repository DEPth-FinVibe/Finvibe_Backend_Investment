package depth.finvibe.investment.modules.trade.infra.persistence;

import depth.finvibe.investment.modules.trade.application.port.out.TradeRepository;
import depth.finvibe.investment.modules.trade.domain.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
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
