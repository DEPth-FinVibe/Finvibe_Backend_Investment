package depth.finvibe.investment.modules.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import depth.finvibe.investment.modules.wallet.domain.Money;
import depth.finvibe.investment.modules.wallet.domain.Wallet;
import depth.finvibe.investment.modules.wallet.domain.error.WalletErrorCode;
import depth.finvibe.investment.modules.wallet.infra.WalletRepository;
import depth.finvibe.investment.shared.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

  @Mock
  private WalletRepository walletRepository;

  @InjectMocks
  private WalletService walletService;

  @Test
  @DisplayName("사용자 ID로 새 지갑을 생성할 수 있다.")
  void createWallet_success() {
    // given
    UUID userId = UUID.randomUUID();
    Wallet savedWallet = new Wallet(1L, userId, new Money(0L));

    when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

    // when
    Wallet result = walletService.createWallet(userId);

    // then
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getBalance().getAmount()).isEqualTo(0L);
    verify(walletRepository, times(1)).save(any(Wallet.class));
  }

  @Test
  @DisplayName("지갑에 금액을 입금할 수 있다.")
  void deposit_success() {
    // given
    Long walletId = 1L;
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, userId, new Money(1000L));
    Long depositAmount = 500L;

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

    // when
    walletService.deposit(walletId, depositAmount);

    // then
    assertThat(wallet.getBalance().getAmount()).isEqualTo(1500L);
    verify(walletRepository, times(1)).findById(walletId);
    // JPA 더티 체킹으로 자동 저장되므로 save() verify 불필요
  }

  @Test
  @DisplayName("존재하지 않는 지갑에 입금하면 WALLET_NOT_FOUND 에러를 발생시킨다.")
  void deposit_walletNotFound() {
    // given
    Long walletId = 999L;
    Long depositAmount = 500L;

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> walletService.deposit(walletId, depositAmount))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", WalletErrorCode.WALLET_NOT_FOUND);
  }

  @Test
  @DisplayName("지갑에서 금액을 출금할 수 있다.")
  void withdraw_success() {
    // given
    Long walletId = 1L;
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, userId, new Money(1000L));
    Long withdrawAmount = 400L;

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

    // when
    walletService.withdraw(walletId, withdrawAmount);

    // then
    assertThat(wallet.getBalance().getAmount()).isEqualTo(600L);
    verify(walletRepository, times(1)).findById(walletId);
    // JPA 더티 체킹으로 자동 저장되므로 save() verify 불필요
  }

  @Test
  @DisplayName("존재하지 않는 지갑에서 출금하면 WALLET_NOT_FOUND 에러를 발생시킨다.")
  void withdraw_walletNotFound() {
    // given
    Long walletId = 999L;
    Long withdrawAmount = 400L;

    when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> walletService.withdraw(walletId, withdrawAmount))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", WalletErrorCode.WALLET_NOT_FOUND);
  }

  @Test
  @DisplayName("잔액보다 많은 금액을 출금하면 INSUFFICIENT_BALANCE 에러를 발생시킨다.")
  void withdraw_insufficientBalance() {
    // given
    Long walletId = 1L;
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, userId, new Money(500L));
    Long withdrawAmount = 1000L;

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

    // when & then
    assertThatThrownBy(() -> walletService.withdraw(walletId, withdrawAmount))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", WalletErrorCode.INSUFFICIENT_BALANCE);
  }

  @Test
  @DisplayName("정확히 현재 잔액과 같은 금액을 출금할 수 있다.")
  void withdraw_exactBalance() {
    // given
    Long walletId = 1L;
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(walletId, userId, new Money(1000L));
    Long withdrawAmount = 1000L;

    when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

    // when
    walletService.withdraw(walletId, withdrawAmount);

    // then
    assertThat(wallet.getBalance().getAmount()).isEqualTo(0L);
    verify(walletRepository, times(1)).findById(walletId);
    // JPA 더티 체킹으로 자동 저장되므로 save() verify 불필요
  }
}
