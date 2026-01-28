package depth.finvibe.investment.modules.asset.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;

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

    public Optional<PortfolioGroup> findDefaultByUserId(UUID userId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(portfolioGroup)
                        .leftJoin(portfolioGroup.assets, asset).fetchJoin()
                        .where(portfolioGroup.userId.eq(userId).and(portfolioGroup.isDefault.eq(true)))
                        .fetchOne()
        );
    }

    public List<PortfolioGroup> findAllWithAssets() {
        return queryFactory
                .selectFrom(portfolioGroup)
                .leftJoin(portfolioGroup.assets, asset).fetchJoin()
                .distinct()
                .fetch();
    }

    public List<PortfolioGroup> findAllByStockIdsWithAssets(List<Long> stockIds) {
        return queryFactory
                .selectFrom(portfolioGroup)
                .leftJoin(portfolioGroup.assets, asset).fetchJoin()
                .where(asset.stockId.in(stockIds))
                .distinct()
                .fetch();
    }

    public boolean existDefaultByUserId(UUID userId) {
        return queryFactory
                .selectFrom(portfolioGroup)
                .where(portfolioGroup.userId.eq(userId).and(portfolioGroup.isDefault.eq(true)))
                .fetchFirst() != null;
    }
}
