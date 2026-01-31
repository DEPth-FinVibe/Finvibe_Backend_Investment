package depth.finvibe.investment.modules.asset.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.investment.modules.asset.application.port.out.UserProfitRankingData;
import depth.finvibe.investment.modules.asset.application.port.out.UserProfitRankingRepository;
import depth.finvibe.investment.modules.asset.application.port.out.UserProfitSnapshotRepository;
import depth.finvibe.investment.modules.asset.domain.UserProfitSnapshotDaily;
import depth.finvibe.investment.modules.asset.domain.enums.UserProfitRankType;

@Service
@RequiredArgsConstructor
public class UserProfitRankingAggregationService {
  private final UserProfitSnapshotRepository userProfitSnapshotRepository;
  private final UserProfitRankingRepository userProfitRankingRepository;

  @Transactional
  public void aggregateWeeklyRankings(LocalDate today) {
    if (today == null) {
      return;
    }
    LocalDate endDate = today.minusDays(1);
    LocalDate startDate = endDate.minusDays(6);
    aggregateRankings(UserProfitRankType.WEEKLY, startDate, endDate);
  }

  @Transactional
  public void aggregateMonthlyRankings(LocalDate today) {
    if (today == null) {
      return;
    }
    LocalDate endDate = today.minusDays(1);
    LocalDate startDate = endDate.withDayOfMonth(1);
    aggregateRankings(UserProfitRankType.MONTHLY, startDate, endDate);
  }

  private void aggregateRankings(UserProfitRankType rankType, LocalDate startDate, LocalDate endDate) {
    List<UserProfitSnapshotDaily> startSnapshots = userProfitSnapshotRepository.findBySnapshotDate(startDate);
    List<UserProfitSnapshotDaily> endSnapshots = userProfitSnapshotRepository.findBySnapshotDate(endDate);

    if (startSnapshots.isEmpty() || endSnapshots.isEmpty()) {
      userProfitRankingRepository.replaceAllRankings(rankType, List.of());
      return;
    }

    Map<UUID, UserProfitSnapshotDaily> startByUser = toUserMap(startSnapshots);
    Map<UUID, UserProfitSnapshotDaily> endByUser = toUserMap(endSnapshots);

    List<UserProfitRankingData> rankings = new ArrayList<>();
    for (Map.Entry<UUID, UserProfitSnapshotDaily> entry : endByUser.entrySet()) {
      UserProfitSnapshotDaily startSnapshot = startByUser.get(entry.getKey());
      if (startSnapshot == null) {
        continue;
      }
      UserProfitSnapshotDaily endSnapshot = entry.getValue();

      BigDecimal periodProfitLoss = endSnapshot.getTotalProfitLoss()
        .subtract(startSnapshot.getTotalProfitLoss());
      BigDecimal periodPurchase = endSnapshot.getTotalCurrentValue()
        .subtract(endSnapshot.getTotalProfitLoss());
      BigDecimal periodReturnRate = calculateReturnRate(periodProfitLoss, periodPurchase);

      rankings.add(new UserProfitRankingData(
        entry.getKey(),
        periodReturnRate,
        periodProfitLoss
      ));
    }

    userProfitRankingRepository.replaceAllRankings(rankType, rankings);
  }

  private Map<UUID, UserProfitSnapshotDaily> toUserMap(List<UserProfitSnapshotDaily> snapshots) {
    Map<UUID, UserProfitSnapshotDaily> map = new HashMap<>();
    for (UserProfitSnapshotDaily snapshot : snapshots) {
      map.put(snapshot.getId().getUserId(), snapshot);
    }
    return map;
  }

  private BigDecimal calculateReturnRate(BigDecimal profitLoss, BigDecimal purchaseAmount) {
    if (purchaseAmount.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return profitLoss
      .divide(purchaseAmount, 4, RoundingMode.HALF_UP)
      .multiply(BigDecimal.valueOf(100));
  }
}
