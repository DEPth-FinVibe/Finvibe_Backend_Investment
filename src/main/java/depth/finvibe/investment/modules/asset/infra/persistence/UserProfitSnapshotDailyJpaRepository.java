package depth.finvibe.investment.modules.asset.infra.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import depth.finvibe.investment.modules.asset.domain.UserProfitSnapshotDaily;
import depth.finvibe.investment.modules.asset.domain.UserProfitSnapshotDailyId;

public interface UserProfitSnapshotDailyJpaRepository
  extends JpaRepository<UserProfitSnapshotDaily, UserProfitSnapshotDailyId> {
  List<UserProfitSnapshotDaily> findByIdSnapshotDate(LocalDate snapshotDate);
}
