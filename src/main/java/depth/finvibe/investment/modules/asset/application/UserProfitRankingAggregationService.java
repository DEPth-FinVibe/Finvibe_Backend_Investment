package depth.finvibe.investment.modules.asset.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
import depth.finvibe.investment.modules.asset.infra.client.UserServiceClient;
import depth.finvibe.investment.shared.application.port.out.GamificationEventProducer;
import depth.finvibe.investment.shared.dto.Badge;
import depth.finvibe.investment.shared.dto.RewardBadgeEvent;

@Service
@RequiredArgsConstructor
public class UserProfitRankingAggregationService {
  private final UserProfitSnapshotRepository userProfitSnapshotRepository;
  private final UserProfitRankingRepository userProfitRankingRepository;
  private final UserServiceClient userServiceClient;
  private final GamificationEventProducer gamificationEventProducer;

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
    Map<UUID, String> userNamesByIds = getUserNamesByIds(endByUser.keySet());

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
        userNamesByIds.get(entry.getKey()),
        periodReturnRate,
        periodProfitLoss
      ));
    }

    userProfitRankingRepository.replaceAllRankings(rankType, rankings);
    publishTopOnePercentTrainerBadges(rankType, rankings);
  }

  private Map<UUID, UserProfitSnapshotDaily> toUserMap(List<UserProfitSnapshotDaily> snapshots) {
    Map<UUID, UserProfitSnapshotDaily> map = new HashMap<>();
    for (UserProfitSnapshotDaily snapshot : snapshots) {
      map.put(snapshot.getId().getUserId(), snapshot);
    }
    return map;
  }

  private Map<UUID, String> getUserNamesByIds(Iterable<UUID> userIds) {
    Map<UUID, String> userNamesByIds = userServiceClient.getUserNkcinamesByIds(userIds);
    if (userNamesByIds == null) {
      return Map.of();
    }
    return userNamesByIds;
  }

  private BigDecimal calculateReturnRate(BigDecimal profitLoss, BigDecimal purchaseAmount) {
    if (purchaseAmount.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return profitLoss
      .divide(purchaseAmount, 4, RoundingMode.HALF_UP)
      .multiply(BigDecimal.valueOf(100));
  }

  private void publishTopOnePercentTrainerBadges(UserProfitRankType rankType, List<UserProfitRankingData> rankings) {
    if (rankType == null || rankings == null || rankings.isEmpty()) {
      return;
    }

    int topCount = Math.max(1, (int) Math.ceil(rankings.size() * 0.01d));
    List<UserProfitRankingData> topRankings = rankings.stream()
      .sorted(Comparator.comparing(
        UserProfitRankingData::totalReturnRate,
        Comparator.nullsLast(BigDecimal::compareTo)
      ).reversed())
      .limit(topCount)
      .toList();

    Instant issuedAt = Instant.now();
    String reason = buildTopOnePercentReason(rankType);
    for (UserProfitRankingData ranking : topRankings) {
      gamificationEventProducer.publishRewardBadgeEvent(RewardBadgeEvent.builder()
        .userId(ranking.userId().toString())
        .badgeCode(Badge.TOP_ONE_PERCENT_TRAINER.name())
        .issuedAt(issuedAt)
        .reason(reason)
        .build());
    }
  }

  private String buildTopOnePercentReason(UserProfitRankType rankType) {
    return switch (rankType) {
      case WEEKLY -> "주간 수익률 상위 1% 선정";
      case MONTHLY -> "월간 수익률 상위 1% 선정";
      case DAILY -> "일간 수익률 상위 1% 선정";
    };
  }
}
