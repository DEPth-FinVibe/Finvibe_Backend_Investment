package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.domain.HoldingStock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingStockJpaRepository extends JpaRepository<HoldingStock, Long> {
    Optional<HoldingStock> findByStockIdAndUserId(Long stockId, UUID userId);
}
