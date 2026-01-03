package depth.finvibe.investment.modules.asset.infra.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static depth.finvibe.investment.modules.asset.domain.QAsset.asset;
import static depth.finvibe.investment.modules.asset.domain.QPortfolioGroup.portfolioGroup;

@Repository
@RequiredArgsConstructor
public class PortfolioGroupQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Optional<PortfolioGroup> findByIdWithAssets(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(portfolioGroup)
                        .leftJoin(portfolioGroup.assets, asset).fetchJoin()
                        .where(portfolioGroup.id.eq(id))
                        .fetchOne()
        );
    }
}
