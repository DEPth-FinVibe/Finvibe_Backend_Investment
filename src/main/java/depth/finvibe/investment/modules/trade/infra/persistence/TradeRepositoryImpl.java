package depth.finvibe.investment.modules.trade.infra.persistence;

import depth.finvibe.investment.modules.trade.application.port.out.TradeRepository;
import depth.finvibe.investment.modules.trade.domain.Trade;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TradeRepositoryImpl implements TradeRepository {

    private final TradeJpaRepository jpaRepository;

    @Override
    public Trade save(Trade trade) {
        return jpaRepository.save(trade);
    }

    @Override
    public Optional<Trade> findById(Long tradeId) {
        return jpaRepository.findById(tradeId);
    }

    @Override
    public List<Long> findDistinctStockIdsByUserIdAndTradeType(UUID userId, TradeType tradeType) {
        return jpaRepository.findDistinctStockIdsByUserIdAndTradeType(userId, tradeType);
    }
}
