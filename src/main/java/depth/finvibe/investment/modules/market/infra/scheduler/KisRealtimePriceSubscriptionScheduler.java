package depth.finvibe.investment.modules.market.infra.scheduler;

import depth.finvibe.investment.modules.market.application.port.out.CurrentStockWatcherRepository;
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

      /**
       * TODO: 종목에 대해 Lock을 걸고 이 노드가 해당 종목의 구독을 담당하도록 수정
       * 1. 종목별로 Lock을 걸어 여러 노드가 동시에 같은 종목을 구독하지 않도록 함
       * 2. Lock 획득에 성공한 노드가 해당 종목의 구독을 담당하도록 함
       * 3. Lock은 TTL이 존재하기 때문에 일정 시간 후 자동 해제됨
       */

    } catch (Exception ex) {
      log.error("KIS 실시간 가격 구독 동기화 실패", ex);
    }
  }
}
