package depth.finvibe.investment.modules.asset.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import depth.finvibe.investment.modules.asset.domain.PortfolioPerformanceSnapshotDaily;
import depth.finvibe.investment.modules.asset.domain.PortfolioPerformanceSnapshotDailyId;

public interface PortfolioPerformanceSnapshotDailyJpaRepository
  extends JpaRepository<PortfolioPerformanceSnapshotDaily, PortfolioPerformanceSnapshotDailyId> {

  List<PortfolioPerformanceSnapshotDaily> findByUserIdAndIdSnapshotDateBetween(
    UUID userId,
    LocalDate startDate,
    LocalDate endDate
  );
}
