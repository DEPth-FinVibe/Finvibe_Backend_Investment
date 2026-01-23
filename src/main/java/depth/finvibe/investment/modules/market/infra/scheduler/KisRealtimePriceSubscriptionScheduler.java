package depth.finvibe.investment.modules.market.infra.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.market.application.port.out.CurrentStockWatcherRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.infra.lock.StockSubscriptionLockManager;
import depth.finvibe.investment.modules.market.infra.websocket.kis.KisConnectionPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisRealtimePriceSubscriptionScheduler {
  private final CurrentStockWatcherRepository currentStockWatcherRepository;
  private final StockRepository stockRepository;
  private final StockSubscriptionLockManager lockManager;
  private final KisConnectionPool kisConnectionPool;

  // 현재 노드가 Lock을 보유한 종목 ID 목록 (Lock 관리용)
  private final Set<Long> lockedStockIds = new HashSet<>();

  private record SubscriptionResult(int successCount, int skipCount, Set<Long> newLockedStockIds) {}

  private record SubscriptionAttempt(boolean isSuccess, boolean isSkipped) {
    static SubscriptionAttempt success() {
      return new SubscriptionAttempt(true, false);
    }

    static SubscriptionAttempt skipped() {
      return new SubscriptionAttempt(false, true);
    }

    static SubscriptionAttempt failed() {
      return new SubscriptionAttempt(false, false);
    }
  }

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
        handleEmptyActiveStocks();
        return;
      }

      Map<Long, String> stockIdToSymbol = buildStockIdToSymbolMap(activeStockIds);
      SubscriptionResult result = processActiveStocks(activeStockIds, stockIdToSymbol);
      cleanupInactiveStocks(activeStockIds, result.newLockedStockIds());

      updateLockedStockIds(result.newLockedStockIds());
      logSyncComplete(result, activeStockIds.size());

    } catch (Exception ex) {
      log.error("KIS 실시간 가격 구독 동기화 실패", ex);
    }
  }

  private void handleEmptyActiveStocks() {
    log.trace("활성 구독 종목이 없어 KIS WebSocket 동기화를 건너뜁니다.");
    unsubscribeAllIfNeeded();
    releaseAllLocks();
  }

  private Map<Long, String> buildStockIdToSymbolMap(List<Long> stockIds) {
    List<Stock> stocks = stockRepository.findAllById(stockIds);
    return stocks.stream().collect(Collectors.toMap(Stock::getId, Stock::getSymbol));
  }

  private SubscriptionResult processActiveStocks(
          List<Long> activeStockIds,
          Map<Long, String> stockIdToSymbol
  ) {
    log.debug("KIS WebSocket 구독 동기화 시작 - 활성 종목 수: {}", activeStockIds.size());

    int successCount = 0;
    int skipCount = 0;
    Set<Long> newLockedStockIds = new HashSet<>();

    for (Long stockId : activeStockIds) {
      SubscriptionAttempt attempt = trySubscribeStock(stockId, stockIdToSymbol, newLockedStockIds);
      if (attempt.isSuccess()) {
        successCount++;
      } else if (attempt.isSkipped()) {
        skipCount++;
      }
    }

    return new SubscriptionResult(successCount, skipCount, newLockedStockIds);
  }

  private SubscriptionAttempt trySubscribeStock(
          Long stockId,
          Map<Long, String> stockIdToSymbol,
          Set<Long> newLockedStockIds
  ) {
    try {
      if (!lockManager.tryAcquireLock(stockId)) {
        return SubscriptionAttempt.skipped();
      }

      newLockedStockIds.add(stockId);
      String symbol = stockIdToSymbol.get(stockId);

      if (symbol == null) {
        log.warn("종목 심볼을 찾을 수 없어 구독을 건너뜁니다 - stockId: {}", stockId);
        return SubscriptionAttempt.failed();
      }

      kisConnectionPool.subscribe(stockId, symbol);
      logNewSubscription(stockId, symbol);
      return SubscriptionAttempt.success();

    } catch (Exception ex) {
      log.error("종목 구독 처리 실패 - stockId: {}", stockId, ex);
      lockManager.releaseLock(stockId);
      newLockedStockIds.remove(stockId);
      return SubscriptionAttempt.failed();
    }
  }

  private void logNewSubscription(Long stockId, String symbol) {
    if (!lockedStockIds.contains(stockId)) {
      log.debug("KIS 실시간 신규 구독 성공 - stockId: {}, symbol: {}", stockId, symbol);
    }
  }

  private void cleanupInactiveStocks(List<Long> activeStockIds, Set<Long> newLockedStockIds) {
    Set<Long> activeStockIdSet = Set.copyOf(activeStockIds);
    Set<Long> subscribedStockIds = kisConnectionPool.getSubscribedStockIds();

    List<Long> inactiveStockIds = subscribedStockIds.stream()
            .filter(stockId -> !activeStockIdSet.contains(stockId))
            .toList();

    if (inactiveStockIds.isEmpty()) {
      return;
    }

    Map<Long, String> inactiveStockIdToSymbol = buildStockIdToSymbolMap(inactiveStockIds);
    unsubscribeStocks(inactiveStockIds, inactiveStockIdToSymbol, newLockedStockIds);
  }

  private void unsubscribeStocks(
          List<Long> stockIds,
          Map<Long, String> stockIdToSymbol,
          Set<Long> newLockedStockIds
  ) {
    int unsubscribeCount = 0;

    for (Long stockId : stockIds) {
      try {
        String symbol = stockIdToSymbol.get(stockId);
        if (symbol != null) {
          kisConnectionPool.unsubscribe(stockId, symbol);
          unsubscribeCount++;
          log.debug("비활성 종목 구독 해제 - stockId: {}, symbol: {}", stockId, symbol);
        }

        lockManager.releaseLock(stockId);
        newLockedStockIds.remove(stockId);
      } catch (Exception ex) {
        log.error("비활성 종목 구독 해제 실패 - stockId: {}", stockId, ex);
      }
    }

    if (unsubscribeCount > 0) {
      log.info("비활성 종목 구독 해제 완료 - 해제 수: {}", unsubscribeCount);
    }
  }

  private void updateLockedStockIds(Set<Long> newLockedStockIds) {
    lockedStockIds.clear();
    lockedStockIds.addAll(newLockedStockIds);
  }

  private void logSyncComplete(SubscriptionResult result, int totalCount) {
    log.info("KIS WebSocket 구독 동기화 완료 - 성공: {}, 스킵(다른 노드): {}, 전체: {}",
            result.successCount(), result.skipCount(), totalCount);
  }

  /**
   * 활성 종목이 없을 때 모든 구독을 해제합니다.
   */
  private void unsubscribeAllIfNeeded() {
    Set<Long> subscribedStockIds = kisConnectionPool.getSubscribedStockIds();
    if (subscribedStockIds.isEmpty()) {
      return;
    }

    log.info("활성 종목이 없어 모든 구독을 해제합니다 - 구독 수: {}", subscribedStockIds.size());
    Map<Long, String> stockIdToSymbol = buildStockIdToSymbolMap(subscribedStockIds.stream().toList());

    for (Long stockId : subscribedStockIds) {
      try {
        String symbol = stockIdToSymbol.get(stockId);
        if (symbol != null) {
          kisConnectionPool.unsubscribe(stockId, symbol);
        }
      } catch (Exception ex) {
        log.error("전체 구독 해제 중 오류 - stockId: {}", stockId, ex);
      }
    }
  }

  /**
   * 현재 노드가 보유한 모든 Lock을 해제합니다.
   */
  private void releaseAllLocks() {
    if (lockedStockIds.isEmpty()) {
      return;
    }

    log.debug("모든 Lock 해제 시작 - Lock 수: {}", lockedStockIds.size());
    for (Long stockId : lockedStockIds) {
      try {
        lockManager.releaseLock(stockId);
      } catch (Exception ex) {
        log.error("Lock 해제 중 오류 - stockId: {}", stockId, ex);
      }
    }
    lockedStockIds.clear();
  }
}
