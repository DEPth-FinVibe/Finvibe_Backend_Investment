package depth.finvibe.investment.modules.asset.infra.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.asset.application.UserProfitRankingAggregationService;

@Component
@RequiredArgsConstructor
public class UserProfitRankingScheduler {
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final UserProfitRankingAggregationService userProfitRankingAggregationService;

  @Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Seoul")
  @SchedulerLock(
    name = "userProfitRankingWeekly",
    lockAtMostFor = "PT10M",
    lockAtLeastFor = "PT10S"
  )
  public void aggregateWeeklyRanking() {
    LocalDate today = LocalDate.now(KST);
    userProfitRankingAggregationService.aggregateWeeklyRankings(today);
  }

  @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Seoul")
  @SchedulerLock(
    name = "userProfitRankingMonthly",
    lockAtMostFor = "PT10M",
    lockAtLeastFor = "PT10S"
  )
  public void aggregateMonthlyRanking() {
    LocalDate today = LocalDate.now(KST);
    userProfitRankingAggregationService.aggregateMonthlyRankings(today);
  }
}
