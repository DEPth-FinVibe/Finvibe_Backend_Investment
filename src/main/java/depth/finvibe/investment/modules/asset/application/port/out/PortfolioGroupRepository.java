package depth.finvibe.investment.modules.asset.application.port.out;

import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;

import java.util.Optional;

public interface PortfolioGroupRepository {
    PortfolioGroup save(PortfolioGroup portfolioGroup);
    Optional<PortfolioGroup> findById(Long id);
}
