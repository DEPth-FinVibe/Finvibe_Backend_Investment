package depth.finvibe.investment.modules.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import depth.finvibe.investment.modules.wallet.domain.error.WalletErrorCode;
import depth.finvibe.investment.shared.error.DomainException;

class MoneyTest {

  @Nested
  @DisplayName("Money 생성 테스트")
  class CreationTest {
    @Test
    @DisplayName("유효한 금액으로 Money를 생성할 수 있다.")
    void createMoney_success() {
      // given
      Long amount = 1000L;

      // when
      Money money = new Money(amount);

      // then
      assertThat(money.getPrice()).isEqualTo(amount);
    }

    @ParameterizedTest
    @ValueSource(longs = { -1L, -100L })
    @DisplayName("음수 금액으로 Money를 생성하면 예외가 발생한다.")
    void createMoney_fail_negativeAmount(Long amount) {
      assertThatThrownBy(() -> new Money(amount))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(WalletErrorCode.INVALID_MONEY_PRICE);
    }

    @Test
    @DisplayName("금액이 null이면 Money 생성 시 예외가 발생한다.")
    void createMoney_fail_nullAmount() {
      assertThatThrownBy(() -> new Money(null))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(WalletErrorCode.INVALID_MONEY_PRICE);
    }
  }

  @Nested
  @DisplayName("Money 연산 테스트")
  class OperationTest {
    @Test
    @DisplayName("두 Money 객체를 더할 수 있다.")
    void plus_success() {
      // given
      Money money1 = new Money(1000L);
      Money money2 = new Money(500L);

      // when
      Money result = money1.plus(money2);

      // then
      assertThat(result.getPrice()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("두 Money 객체를 뺄 수 있다.")
    void minus_success() {
      // given
      Money money1 = new Money(1000L);
      Money money2 = new Money(400L);

      // when
      Money result = money1.minus(money2);

      // then
      assertThat(result.getPrice()).isEqualTo(600L);
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 빼면 예외가 발생한다.")
    void minus_fail_insufficientBalance() {
      // given
      Money money1 = new Money(1000L);
      Money money2 = new Money(1500L);

      // when & then
      assertThatThrownBy(() -> money1.minus(money2))
          .isInstanceOf(DomainException.class)
          .extracting("errorCode")
          .isEqualTo(WalletErrorCode.INSUFFICIENT_BALANCE);
    }
  }
}
