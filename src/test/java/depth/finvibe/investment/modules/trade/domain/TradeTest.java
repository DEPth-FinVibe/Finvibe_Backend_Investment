package depth.finvibe.investment.modules.trade.domain;

import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import depth.finvibe.investment.modules.trade.domain.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Trade 도메인 테스트")
class TradeTest {

  @Test
  @DisplayName("거래 생성 성공")
  void createTrade_success() {
    // given
    Long stockId = 1L;
    Double amount = 10.0;
    Long price = 50000L;
    Long portfolioId = 1L;
    UUID userId = UUID.randomUUID();

    // when
    Trade trade = Trade.create(
        stockId,
        amount,
        price,
        portfolioId,
        userId,
        TransactionType.BUY,
        TradeType.NORMAL,
        "테스트종목"
    );

    // then
    assertThat(trade.getStockId()).isEqualTo(stockId);
    assertThat(trade.getAmount()).isEqualTo(amount);
    assertThat(trade.getPrice()).isEqualTo(price);
    assertThat(trade.getPortfolioId()).isEqualTo(portfolioId);
    assertThat(trade.getUserId()).isEqualTo(userId);
    assertThat(trade.getTransactionType()).isEqualTo(TransactionType.BUY);
    assertThat(trade.getTradeType()).isEqualTo(TradeType.NORMAL);
  }

  @Test
  @DisplayName("예약 주문 취소 성공")
  void cancelReservedTrade_success() {
    // given
    Trade trade = Trade.create(
        1L,
        10.0,
        50000L,
        1L,
        UUID.randomUUID(),
        TransactionType.BUY,
        TradeType.RESERVED,
        "테스트종목"
    );

    // when
    trade.cancel();

    // then
    assertThat(trade.getTradeType()).isEqualTo(TradeType.CANCELLED);
  }

  @Test
  @DisplayName("예약 주문 체결 성공")
  void executeReservedTrade_success() {
    // given
    Trade trade = Trade.create(
        1L,
        10.0,
        50000L,
        1L,
        UUID.randomUUID(),
        TransactionType.BUY,
        TradeType.RESERVED,
        "테스트종목"
    );

    // when
    trade.execute();

    // then
    assertThat(trade.getTradeType()).isEqualTo(TradeType.NORMAL);
  }

  @ParameterizedTest
  @EnumSource(TransactionType.class)
  @DisplayName("모든 TransactionType으로 거래 생성 가능")
  void createTradeWithAllTransactionTypes(TransactionType transactionType) {
    // when
    Trade trade = Trade.create(
        1L,
        10.0,
        50000L,
        1L,
        UUID.randomUUID(),
        transactionType,
        TradeType.NORMAL,
        "테스트종목"
    );

    // then
    assertThat(trade.getTransactionType()).isEqualTo(transactionType);
  }

  @ParameterizedTest
  @EnumSource(TradeType.class)
  @DisplayName("모든 TradeType으로 거래 생성 가능")
  void createTradeWithAllTradeTypes(TradeType tradeType) {
    // when
    Trade trade = Trade.create(
        1L,
        10.0,
        50000L,
        1L,
        UUID.randomUUID(),
        TransactionType.BUY,
        tradeType,
        "테스트종목"
    );

    // then
    assertThat(trade.getTradeType()).isEqualTo(tradeType);
  }
}
