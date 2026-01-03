package depth.finvibe.investment.modules.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import depth.finvibe.investment.modules.asset.domain.error.AssetErrorCode;
import depth.finvibe.investment.shared.error.DomainException;

class MoneyTest {

  @Test
  @DisplayName("음수 금액으로 Money를 만들면 예외가 발생한다.")
  void createMoney_negative_fail() {
    assertThatThrownBy(() -> Money.of(-1d, Currency.KRW))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.NEGATIVE_MONEY_AMOUNT));
  }

  @Test
  @DisplayName("통화가 없으면 Money 생성 시 예외가 발생한다.")
  void createMoney_withoutCurrency_fail() {
    assertThatThrownBy(() -> Money.of(1d, null))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.INVALID_MONEY_PARAMS));
  }

  @Test
  @DisplayName("다른 통화끼리 더하면 예외가 발생한다.")
  void plus_differentCurrency_fail() {
    Money krw = Money.of(1_000d, Currency.KRW);
    Money usd = Money.of(1d, Currency.USD);

    assertThatThrownBy(() -> krw.plus(usd))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.CANNOT_ADD_DIFFERENT_CURRENCIES));
  }

  @Test
  @DisplayName("다른 통화끼리 차감하면 예외가 발생한다.")
  void minus_differentCurrency_fail() {
    Money krw = Money.of(1_000d, Currency.KRW);
    Money usd = Money.of(1d, Currency.USD);

    assertThatThrownBy(() -> krw.minus(usd))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.CANNOT_SUBTRACT_DIFFERENT_CURRENCIES));
  }

  @Test
  @DisplayName("차감 결과가 음수가 되면 예외가 발생한다.")
  void minus_negativeResult_fail() {
    Money krw = Money.of(1_000d, Currency.KRW);
    Money bigger = Money.of(2_000d, Currency.KRW);

    assertThatThrownBy(() -> krw.minus(bigger))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.NEGATIVE_MONEY_AMOUNT));
  }
}
