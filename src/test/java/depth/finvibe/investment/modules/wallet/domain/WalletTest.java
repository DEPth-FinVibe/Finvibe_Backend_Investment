package depth.finvibe.investment.modules.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class WalletTest {

  @Test
  @DisplayName("지갑에 금액을 입금할 수 있다.")
  void deposit_success() {
    // given
    Wallet wallet = new Wallet(1L, UUID.randomUUID(), new Money(1000L));
    Money depositAmount = new Money(500L);

    // when
    wallet.deposit(depositAmount);

    // then
    assertThat(wallet.getBalance().getPrice()).isEqualTo(1500L);
  }

  @Test
  @DisplayName("지갑에서 금액을 출금할 수 있다.")
  void withdraw_success() {
    // given
    Wallet wallet = new Wallet(1L, UUID.randomUUID(), new Money(1000L));
    Money withdrawAmount = new Money(400L);

    // when
    wallet.withdraw(withdrawAmount);

    // then
    assertThat(wallet.getBalance().getPrice()).isEqualTo(600L);
  }
}
