package depth.finvibe.investment.modules.asset.application.port.out;

import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioGroupRepository {
    PortfolioGroup save(PortfolioGroup portfolioGroup);
    Optional<PortfolioGroup> findById(Long id);
    List<PortfolioGroup> findAllByUserId(UUID userId);
}
