package depth.finvibe.investment.modules.asset.infra.persistence;

import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioGroupJpaRepository extends JpaRepository<PortfolioGroup, Long> {
}
