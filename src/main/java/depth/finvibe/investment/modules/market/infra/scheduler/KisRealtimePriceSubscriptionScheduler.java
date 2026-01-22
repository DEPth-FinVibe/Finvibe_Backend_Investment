package depth.finvibe.investment.modules.market.infra.scheduler;

import depth.finvibe.investment.modules.market.application.port.out.CurrentStockWatcherRepository;
import depth.finvibe.investment.modules.market.infra.websocket.kis.legacy.KisRealtimePriceSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisRealtimePriceSubscriptionScheduler {
  private final CurrentStockWatcherRepository currentStockWatcherRepository;
  private final KisRealtimePriceSubscriber subscriber;

  @Scheduled(fixedDelayString = "${market.kis.websocket.sync-interval-ms:5000}")
  @SchedulerLock(
          name = "kisRealtimePriceSubscriptionSync",
          lockAtMostFor = "PT30S",
          lockAtLeastFor = "PT1S"
  )
  public void syncRealtimeSubscriptions() {
    try {
      List<Long> activeStockIds = currentStockWatcherRepository.findActiveStockIds();
      if (activeStockIds.isEmpty()) {
        log.trace("활성 구독 종목이 없어 KIS WebSocket 동기화를 건너뜁니다.");
        return;
      }
      subscriber.syncSubscriptions(activeStockIds);
    } catch (Exception ex) {
      log.error("KIS 실시간 가격 구독 동기화 실패", ex);
    }
  }
}
