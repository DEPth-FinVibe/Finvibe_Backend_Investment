package depth.finvibe.investment.modules.market.infra.scheduler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.market.application.port.out.CurrentStockWatcherRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.infra.lock.ActiveNodeRegistry;
import depth.finvibe.investment.modules.market.infra.lock.StockSubscriptionLockManager;
import depth.finvibe.investment.modules.market.infra.websocket.kis.KisConnectionPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * KIS WebSocket 실시간 가격 구독 상태를 동기화하는 스케줄러입니다.
 * 
 * 여러 노드 환경에서 공평한 부하 분산을 위해:
 * 1. Heartbeat 방식으로 활성 노드 수를 파악
 * 2. 노드당 최대 구독 수를 동적으로 계산
 * 3. FIFO 방식으로 초과 구독 해제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisSubscriptionSynchronizer {
  private static final int MAX_SUBSCRIPTIONS_PER_SESSION = 41;

  private final CurrentStockWatcherRepository currentStockWatcherRepository;
  private final StockRepository stockRepository;
  private final StockSubscriptionLockManager lockManager;
  private final KisConnectionPool kisConnectionPool;
  private final ActiveNodeRegistry activeNodeRegistry;

  // FIFO 방식으로 구독 순서를 추적 (LinkedHashSet)
  private final LinkedHashSet<Long> subscriptionOrder = new LinkedHashSet<>();

  private record SubscriptionResult(int successCount, int skipCount, int releasedCount) {}

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
      // Heartbeat 기록
      activeNodeRegistry.recordHeartbeat();

      List<Long> activeStockIds = currentStockWatcherRepository.findActiveStockIds();

      if (activeStockIds.isEmpty()) {
        handleEmptyActiveStocks();
        return;
      }

      // 노드당 최대 구독 수 계산
      int maxSubscriptionsForNode = calculateMaxSubscriptionsForNode(activeStockIds.size());

      Map<Long, String> stockIdToSymbol = buildStockIdToSymbolMap(activeStockIds);
      SubscriptionResult result = processActiveStocks(activeStockIds, stockIdToSymbol, maxSubscriptionsForNode);
      cleanupInactiveStocks(activeStockIds);

      logSyncComplete(result, activeStockIds.size(), maxSubscriptionsForNode);

    } catch (Exception ex) {
      log.error("KIS 실시간 가격 구독 동기화 실패", ex);
    }
  }

  /**
   * 노드당 최대 구독 수를 계산합니다.
   *
   * @param totalActiveStocks 전체 활성 종목 수
   * @return 현재 노드가 보유할 수 있는 최대 구독 수
   */
  private int calculateMaxSubscriptionsForNode(int totalActiveStocks) {
    int activeNodeCount = activeNodeRegistry.getActiveNodeCount();
    int availableSessionCount = kisConnectionPool.getAvailableSessionCount();

    if (availableSessionCount == 0) {
      log.warn("사용 가능한 KIS 세션이 없습니다. 구독을 중단합니다.");
      return 0;
    }

    // 세션당 최대 구독 수 제한
    int maxBySession = availableSessionCount * MAX_SUBSCRIPTIONS_PER_SESSION;

    // 노드 간 공평 분배
    int fairShare = (int) Math.ceil((double) totalActiveStocks / activeNodeCount);

    int maxSubscriptions = Math.min(fairShare, maxBySession);

    log.debug("노드당 최대 구독 수 계산 - 활성 노드: {}, 전체 종목: {}, 세션 수: {}, 할당량: {}",
            activeNodeCount, totalActiveStocks, availableSessionCount, maxSubscriptions);

    return maxSubscriptions;
  }

  private void handleEmptyActiveStocks() {
    log.trace("활성 구독 종목이 없어 KIS WebSocket 동기화를 건너뜁니다.");
    unsubscribeAllIfNeeded();
    releaseAllSubscriptions();
  }

  private Map<Long, String> buildStockIdToSymbolMap(List<Long> stockIds) {
    List<Stock> stocks = stockRepository.findAllById(stockIds);
    return stocks.stream().collect(Collectors.toMap(Stock::getId, Stock::getSymbol));
  }

  private SubscriptionResult processActiveStocks(
          List<Long> activeStockIds,
          Map<Long, String> stockIdToSymbol,
          int maxSubscriptionsForNode
  ) {
    log.debug("KIS WebSocket 구독 동기화 시작 - 활성 종목 수: {}, 최대 구독 수: {}",
            activeStockIds.size(), maxSubscriptionsForNode);

    int successCount = 0;
    int skipCount = 0;
    int releasedCount = 0;

    // 1. 초과 구독 해제 (FIFO 방식)
    releasedCount = releaseExcessSubscriptions(maxSubscriptionsForNode, stockIdToSymbol);

    // 2. 신규 구독 처리
    for (Long stockId : activeStockIds) {
      // 최대 구독 수 체크
      if (subscriptionOrder.size() >= maxSubscriptionsForNode) {
        log.debug("노드 최대 구독 수 도달 - 현재: {}, 최대: {}", subscriptionOrder.size(), maxSubscriptionsForNode);
        break;
      }

      SubscriptionAttempt attempt = trySubscribeStock(stockId, stockIdToSymbol);
      if (attempt.isSuccess()) {
        successCount++;
      } else if (attempt.isSkipped()) {
        skipCount++;
      }
    }

    return new SubscriptionResult(successCount, skipCount, releasedCount);
  }

  /**
   * 초과 구독을 FIFO 방식으로 해제합니다.
   *
   * @param maxSubscriptions 최대 구독 수
   * @param stockIdToSymbol 종목 ID-심볼 매핑
   * @return 해제된 구독 수
   */
  private int releaseExcessSubscriptions(int maxSubscriptions, Map<Long, String> stockIdToSymbol) {
    int releasedCount = 0;

    while (subscriptionOrder.size() > maxSubscriptions) {
      Long oldestStockId = subscriptionOrder.iterator().next();
      String symbol = stockIdToSymbol.get(oldestStockId);

      if (symbol == null) {
        // 심볼을 찾을 수 없으면 DB에서 조회
        symbol = stockRepository.findById(oldestStockId)
                .map(Stock::getSymbol)
                .orElse(null);
      }

      if (symbol != null) {
        try {
          kisConnectionPool.unsubscribe(oldestStockId, symbol);
          lockManager.releaseLock(oldestStockId);
          subscriptionOrder.remove(oldestStockId);
          releasedCount++;
          log.info("할당량 초과로 구독 해제 (FIFO) - stockId: {}, symbol: {}, 남은 구독: {}",
                  oldestStockId, symbol, subscriptionOrder.size());
        } catch (Exception ex) {
          log.error("초과 구독 해제 실패 - stockId: {}", oldestStockId, ex);
          subscriptionOrder.remove(oldestStockId); // 실패해도 추적 목록에서 제거
        }
      } else {
        log.warn("초과 구독 해제 중 심볼을 찾을 수 없음 - stockId: {}", oldestStockId);
        subscriptionOrder.remove(oldestStockId);
      }
    }

    return releasedCount;
  }

  private SubscriptionAttempt trySubscribeStock(
          Long stockId,
          Map<Long, String> stockIdToSymbol
  ) {
    // 이미 구독 중인 경우 스킵 (멱등성)
    if (subscriptionOrder.contains(stockId)) {
      return SubscriptionAttempt.skipped();
    }

    try {
      if (!lockManager.tryAcquireLock(stockId)) {
        return SubscriptionAttempt.skipped();
      }

      String symbol = stockIdToSymbol.get(stockId);

      if (symbol == null) {
        log.warn("종목 심볼을 찾을 수 없어 구독을 건너뜁니다 - stockId: {}", stockId);
        lockManager.releaseLock(stockId);
        return SubscriptionAttempt.failed();
      }

      kisConnectionPool.subscribe(stockId, symbol);
      subscriptionOrder.add(stockId);
      logNewSubscription(stockId, symbol);
      return SubscriptionAttempt.success();

    } catch (Exception ex) {
      log.error("종목 구독 처리 실패 - stockId: {}", stockId, ex);
      lockManager.releaseLock(stockId);
      subscriptionOrder.remove(stockId);
      return SubscriptionAttempt.failed();
    }
  }

  private void logNewSubscription(Long stockId, String symbol) {
    if (!kisConnectionPool.isSubscribed(stockId)) {
      log.debug("KIS 실시간 신규 구독 성공 - stockId: {}, symbol: {}", stockId, symbol);
    }
  }

  private void cleanupInactiveStocks(List<Long> activeStockIds) {
    Set<Long> activeStockIdSet = Set.copyOf(activeStockIds);
    Set<Long> subscribedStockIds = kisConnectionPool.getSubscribedStockIds();

    List<Long> inactiveStockIds = subscribedStockIds.stream()
            .filter(stockId -> !activeStockIdSet.contains(stockId))
            .toList();

    if (inactiveStockIds.isEmpty()) {
      return;
    }

    Map<Long, String> inactiveStockIdToSymbol = buildStockIdToSymbolMap(inactiveStockIds);
    unsubscribeStocks(inactiveStockIds, inactiveStockIdToSymbol);
  }

  private void unsubscribeStocks(
          List<Long> stockIds,
          Map<Long, String> stockIdToSymbol
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
        subscriptionOrder.remove(stockId);
      } catch (Exception ex) {
        log.error("비활성 종목 구독 해제 실패 - stockId: {}", stockId, ex);
      }
    }

    if (unsubscribeCount > 0) {
      log.info("비활성 종목 구독 해제 완료 - 해제 수: {}", unsubscribeCount);
    }
  }

  private void logSyncComplete(SubscriptionResult result, int totalCount, int maxSubscriptions) {
    log.info("KIS WebSocket 구독 동기화 완료 - 성공: {}, 스킵(다른 노드): {}, 해제(FIFO): {}, 전체: {}, 최대: {}, 현재: {}",
            result.successCount(), result.skipCount(), result.releasedCount(),
            totalCount, maxSubscriptions, subscriptionOrder.size());
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
        lockManager.releaseLock(stockId);
      } catch (Exception ex) {
        log.error("전체 구독 해제 중 오류 - stockId: {}", stockId, ex);
      }
    }
  }

  /**
   * 현재 노드가 보유한 모든 구독을 해제합니다.
   */
  private void releaseAllSubscriptions() {
    if (subscriptionOrder.isEmpty()) {
      return;
    }

    log.debug("모든 구독 해제 시작 - 구독 수: {}", subscriptionOrder.size());
    for (Long stockId : List.copyOf(subscriptionOrder)) {
      try {
        lockManager.releaseLock(stockId);
      } catch (Exception ex) {
        log.error("구독 해제 중 오류 - stockId: {}", stockId, ex);
      }
    }
    subscriptionOrder.clear();
  }
}
