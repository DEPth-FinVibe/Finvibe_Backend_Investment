package depth.finvibe.investment.modules.market.infra.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.market.application.BatchPriceUpdateService;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchPriceUpdateScheduler {

  private final BatchPriceUpdateService batchPriceUpdateService;

  @Scheduled(cron = "0 0 * * * *")
  @SchedulerLock(
          name = "batchPriceUpdate",
          lockAtMostFor = "PT10M",
          lockAtLeastFor = "PT1M"
  )
  public void executeBatchPriceUpdate() {
    if (!isMarketHours()) {
      log.debug("Skipping batch price update - outside market hours");
      return;
    }

    log.info("Starting scheduled batch price update");
    try {
      batchPriceUpdateService.updateHoldingStockPrices();
      log.info("Completed scheduled batch price update");
    } catch (Exception e) {
      log.error("Failed to execute batch price update", e);
    }
  }

  private boolean isMarketHours() {
    LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    DayOfWeek dayOfWeek = now.getDayOfWeek();
    LocalTime time = now.toLocalTime();

    return dayOfWeek != DayOfWeek.SATURDAY
            && dayOfWeek != DayOfWeek.SUNDAY
            && !time.isBefore(LocalTime.of(9, 0))
            && !time.isAfter(LocalTime.of(15, 30));
  }
}
