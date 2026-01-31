package depth.finvibe.investment.modules.asset.infra.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import depth.finvibe.investment.modules.asset.application.port.out.UserProfitSnapshotRepository;
import depth.finvibe.investment.modules.asset.domain.UserProfitSnapshotDaily;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserProfitSnapshotRepositoryImpl implements UserProfitSnapshotRepository {
  private final UserProfitSnapshotDailyJpaRepository jpaRepository;

  @Override
  public void saveAll(List<UserProfitSnapshotDaily> snapshots) {
    if (snapshots == null || snapshots.isEmpty()) {
      return;
    }
    jpaRepository.saveAll(snapshots);
  }

  @Override
  public List<UserProfitSnapshotDaily> findBySnapshotDate(LocalDate snapshotDate) {
    if (snapshotDate == null) {
      return List.of();
    }
    return jpaRepository.findByIdSnapshotDate(snapshotDate);
  }
}
