package depth.finvibe.investment.modules.asset.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;

public interface PortfolioGroupRepository {
    PortfolioGroup save(PortfolioGroup portfolioGroup);

    Optional<PortfolioGroup> findById(Long id);

    Optional<PortfolioGroup> findByIdWithAssets(Long id);

    List<PortfolioGroup> findAllByUserId(UUID userId);

    Optional<PortfolioGroup> findDefaultByUserId(UUID userId);

    List<PortfolioGroup> findAllByStockIdsWithAssets(List<Long> stockIds);

    void delete(PortfolioGroup existing);

    boolean existDefaultByUserId(UUID userId);
}
