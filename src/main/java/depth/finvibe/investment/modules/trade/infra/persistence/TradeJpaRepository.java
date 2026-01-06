package depth.finvibe.investment.modules.trade.infra.persistence;

import depth.finvibe.investment.modules.trade.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeJpaRepository extends JpaRepository<Trade, Long> {
}
