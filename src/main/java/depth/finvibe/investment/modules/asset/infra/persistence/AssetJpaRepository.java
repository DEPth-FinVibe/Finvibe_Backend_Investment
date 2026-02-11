package depth.finvibe.investment.modules.asset.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import depth.finvibe.investment.modules.asset.domain.Asset;

public interface AssetJpaRepository extends JpaRepository<Asset, Long> {
}
