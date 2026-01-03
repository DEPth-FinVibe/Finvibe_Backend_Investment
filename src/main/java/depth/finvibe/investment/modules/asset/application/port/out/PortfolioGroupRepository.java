package depth.finvibe.investment.modules.asset.application.port.out;

import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioGroupRepository {
    PortfolioGroup save(PortfolioGroup portfolioGroup);

    Optional<PortfolioGroup> findById(Long id);

    Optional<PortfolioGroup> findByIdWithAssets(Long id);

    List<PortfolioGroup> findAllByUserId(UUID userId);

    Optional<PortfolioGroup> findDefaultByUserId(UUID userId);

    void delete(PortfolioGroup existing);

    boolean existDefaultByUserId(UUID userId);
}
