package depth.finvibe.investment.modules.market.infra.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import depth.finvibe.investment.modules.market.application.port.out.CurrentStockWatcherRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.infra.lock.StockSubscriptionLockManager;
import depth.finvibe.investment.modules.market.infra.websocket.kis.KisConnectionPool;

@ExtendWith(MockitoExtension.class)
class KisRealtimePriceSubscriptionSchedulerTest {

  @Mock
  private CurrentStockWatcherRepository currentStockWatcherRepository;

  @Mock
  private StockRepository stockRepository;

  @Mock
  private StockSubscriptionLockManager lockManager;

  @Mock
  private KisConnectionPool kisConnectionPool;

  @InjectMocks
  private KisRealtimePriceSubscriptionScheduler scheduler;

  @Test
  @DisplayName("활성 종목이 없으면 구독을 시도하지 않는다")
  void syncRealtimeSubscriptions_noActiveStocks() {
    // Given
    when(currentStockWatcherRepository.findActiveStockIds()).thenReturn(List.of());
    when(kisConnectionPool.getSubscribedStockIds()).thenReturn(Set.of());

    // When
    scheduler.syncRealtimeSubscriptions();

    // Then
    verify(stockRepository, never()).findAllById(any());
    verify(lockManager, never()).tryAcquireLock(anyLong());
    verify(kisConnectionPool, never()).subscribe(anyLong(), any());
  }

  @Test
  @DisplayName("Lock 획득에 성공하면 KIS WebSocket 구독을 시도한다")
  void syncRealtimeSubscriptions_lockAcquired_subscribe() {
    // Given
    Long stockId = 1L;
    String symbol = "005930";
    Stock stock = Stock.builder()
            .id(stockId)
            .symbol(symbol)
            .name("삼성전자")
            .build();

    when(currentStockWatcherRepository.findActiveStockIds()).thenReturn(List.of(stockId));
    when(stockRepository.findAllById(List.of(stockId))).thenReturn(List.of(stock));
    when(lockManager.tryAcquireLock(stockId)).thenReturn(true);
    when(kisConnectionPool.getSubscribedStockIds()).thenReturn(Set.of());

    // When
    scheduler.syncRealtimeSubscriptions();

    // Then
    verify(lockManager).tryAcquireLock(stockId);
    verify(kisConnectionPool).subscribe(stockId, symbol);
  }

  @Test
  @DisplayName("Lock 획득에 실패하면 KIS WebSocket 구독을 건너뛴다")
  void syncRealtimeSubscriptions_lockFailed_skipSubscribe() {
    // Given
    Long stockId = 1L;
    String symbol = "005930";
    Stock stock = Stock.builder()
            .id(stockId)
            .symbol(symbol)
            .name("삼성전자")
            .build();

    when(currentStockWatcherRepository.findActiveStockIds()).thenReturn(List.of(stockId));
    when(stockRepository.findAllById(List.of(stockId))).thenReturn(List.of(stock));
    when(lockManager.tryAcquireLock(stockId)).thenReturn(false);
    when(kisConnectionPool.getSubscribedStockIds()).thenReturn(Set.of());

    // When
    scheduler.syncRealtimeSubscriptions();

    // Then
    verify(lockManager).tryAcquireLock(stockId);
    verify(kisConnectionPool, never()).subscribe(anyLong(), any());
  }

  @Test
  @DisplayName("여러 종목 중 Lock 획득에 성공한 종목만 구독한다")
  void syncRealtimeSubscriptions_multipleStocks_partialLockSuccess() {
    // Given
    Stock stock1 = Stock.builder().id(1L).symbol("005930").name("삼성전자").build();
    Stock stock2 = Stock.builder().id(2L).symbol("000660").name("SK하이닉스").build();
    Stock stock3 = Stock.builder().id(3L).symbol("035720").name("카카오").build();

    when(currentStockWatcherRepository.findActiveStockIds()).thenReturn(List.of(1L, 2L, 3L));
    when(stockRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(stock1, stock2, stock3));
    when(lockManager.tryAcquireLock(1L)).thenReturn(true);
    when(lockManager.tryAcquireLock(2L)).thenReturn(false);
    when(lockManager.tryAcquireLock(3L)).thenReturn(true);
    when(kisConnectionPool.getSubscribedStockIds()).thenReturn(Set.of());

    // When
    scheduler.syncRealtimeSubscriptions();

    // Then
    verify(kisConnectionPool).subscribe(1L, "005930");
    verify(kisConnectionPool, never()).subscribe(2L, "000660");
    verify(kisConnectionPool).subscribe(3L, "035720");
  }

  @Test
  @DisplayName("비활성화된 종목은 구독 해제한다")
  void syncRealtimeSubscriptions_unsubscribeInactiveStocks() {
    // Given
    Stock stock1 = Stock.builder().id(1L).symbol("005930").name("삼성전자").build();
    Stock stock2 = Stock.builder().id(2L).symbol("000660").name("SK하이닉스").build();

    // 현재 구독 중: 1L, 2L / 활성 종목: 1L만 활성
    when(currentStockWatcherRepository.findActiveStockIds()).thenReturn(List.of(1L));
    when(stockRepository.findAllById(any())).thenReturn(List.of(stock1, stock2));
    when(lockManager.tryAcquireLock(1L)).thenReturn(true);
    when(kisConnectionPool.getSubscribedStockIds()).thenReturn(Set.of(1L, 2L));

    // When
    scheduler.syncRealtimeSubscriptions();

    // Then
    verify(kisConnectionPool).subscribe(1L, "005930");
    verify(kisConnectionPool).unsubscribe(2L, "000660");
  }

  @Test
  @DisplayName("활성 종목이 없으면 모든 구독을 해제한다")
  void syncRealtimeSubscriptions_noActiveStocks_unsubscribeAll() {
    // Given
    Stock stock1 = Stock.builder().id(1L).symbol("005930").name("삼성전자").build();
    Stock stock2 = Stock.builder().id(2L).symbol("000660").name("SK하이닉스").build();

    when(currentStockWatcherRepository.findActiveStockIds()).thenReturn(List.of());
    when(kisConnectionPool.getSubscribedStockIds()).thenReturn(Set.of(1L, 2L));
    when(stockRepository.findAllById(any())).thenReturn(List.of(stock1, stock2));

    // When
    scheduler.syncRealtimeSubscriptions();

    // Then
    verify(kisConnectionPool).unsubscribe(1L, "005930");
    verify(kisConnectionPool).unsubscribe(2L, "000660");
  }

  @Test
  @DisplayName("구독 중 예외가 발생해도 나머지 종목 처리를 계속한다")
  void syncRealtimeSubscriptions_exceptionDuringSubscribe_continueProcessing() {
    // Given
    Stock stock1 = Stock.builder().id(1L).symbol("005930").name("삼성전자").build();
    Stock stock2 = Stock.builder().id(2L).symbol("000660").name("SK하이닉스").build();

    when(currentStockWatcherRepository.findActiveStockIds()).thenReturn(List.of(1L, 2L));
    when(stockRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(stock1, stock2));
    when(lockManager.tryAcquireLock(1L)).thenReturn(true);
    when(lockManager.tryAcquireLock(2L)).thenReturn(true);
    when(kisConnectionPool.getSubscribedStockIds()).thenReturn(Set.of());

    // 첫 번째 구독에서 예외 발생
    doThrow(new RuntimeException("Network error"))
            .when(kisConnectionPool).subscribe(eq(1L), eq("005930"));

    // When & Then
    assertThatCode(() -> scheduler.syncRealtimeSubscriptions())
            .doesNotThrowAnyException();

    // 두 번째 종목은 정상 처리되어야 함
    verify(kisConnectionPool).subscribe(2L, "000660");
  }
}
