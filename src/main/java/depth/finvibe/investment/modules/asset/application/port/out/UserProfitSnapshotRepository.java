package depth.finvibe.investment.modules.asset.application.port.out;

import java.time.LocalDate;
import java.util.List;

import depth.finvibe.investment.modules.asset.domain.UserProfitSnapshotDaily;

public interface UserProfitSnapshotRepository {
  void saveAll(List<UserProfitSnapshotDaily> snapshots);

  List<UserProfitSnapshotDaily> findBySnapshotDate(LocalDate snapshotDate);
}
