package depth.finvibe.investment.modules.asset.infra.persistence;

import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PortfolioGroupRepositoryImpl implements PortfolioGroupRepository {
    private final PortfolioGroupJpaRepository jpaRepository;

    @Override
    public PortfolioGroup save(PortfolioGroup portfolioGroup) {
        return jpaRepository.save(portfolioGroup);
    }

    @Override
    public Optional<PortfolioGroup> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<PortfolioGroup> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId);
    }
}
