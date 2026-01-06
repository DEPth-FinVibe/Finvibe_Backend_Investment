package depth.finvibe.investment.modules.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import depth.finvibe.investment.modules.wallet.application.port.out.WalletRepository;
import depth.finvibe.investment.modules.wallet.dto.WalletDto;
import depth.finvibe.investment.modules.wallet.domain.Money;
import depth.finvibe.investment.modules.wallet.domain.Wallet;
import depth.finvibe.investment.modules.wallet.domain.error.WalletErrorCode;
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
    WalletDto.WalletResponse result = walletService.createWallet(userId);

    // then
    assertThat(result.getWalletId()).isEqualTo(1L);
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getBalance()).isEqualTo(0L);
    verify(walletRepository, times(1)).save(any(Wallet.class));
  }

  @Test
  @DisplayName("사용자 ID로 지갑에 금액을 입금할 수 있다.")
  void deposit_success() {
    // given
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(1L, userId, new Money(1000L));
    Long depositAmount = 500L;

    when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

    // when
    WalletDto.WalletResponse result = walletService.deposit(userId, depositAmount);

    // then
    assertThat(wallet.getBalance().getPrice()).isEqualTo(1500L);
    assertThat(result.getBalance()).isEqualTo(1500L);
    verify(walletRepository, times(1)).findByUserId(userId);
    // JPA 더티 체킹으로 자동 저장되므로 save() verify 불필요
  }

  @Test
  @DisplayName("존재하지 않는 사용자 ID로 입금하면 WALLET_NOT_FOUND 에러를 발생시킨다.")
  void deposit_walletNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    Long depositAmount = 500L;

    when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> walletService.deposit(userId, depositAmount))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", WalletErrorCode.WALLET_NOT_FOUND);
  }

  @Test
  @DisplayName("사용자 ID로 지갑에서 금액을 출금할 수 있다.")
  void withdraw_success() {
    // given
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(1L, userId, new Money(1000L));
    Long withdrawAmount = 400L;

    when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

    // when
    WalletDto.WalletResponse result = walletService.withdraw(userId, withdrawAmount);

    // then
    assertThat(wallet.getBalance().getPrice()).isEqualTo(600L);
    assertThat(result.getBalance()).isEqualTo(600L);
    verify(walletRepository, times(1)).findByUserId(userId);
    // JPA 더티 체킹으로 자동 저장되므로 save() verify 불필요
  }

  @Test
  @DisplayName("존재하지 않는 사용자 ID로 출금하면 WALLET_NOT_FOUND 에러를 발생시킨다.")
  void withdraw_walletNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    Long withdrawAmount = 400L;

    when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> walletService.withdraw(userId, withdrawAmount))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", WalletErrorCode.WALLET_NOT_FOUND);
  }

  @Test
  @DisplayName("잔액보다 많은 금액을 출금하면 INSUFFICIENT_BALANCE 에러를 발생시킨다.")
  void withdraw_insufficientBalance() {
    // given
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(1L, userId, new Money(500L));
    Long withdrawAmount = 1000L;

    when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

    // when & then
    assertThatThrownBy(() -> walletService.withdraw(userId, withdrawAmount))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("errorCode", WalletErrorCode.INSUFFICIENT_BALANCE);
  }

  @Test
  @DisplayName("정확히 현재 잔액과 같은 금액을 사용자 ID로 출금할 수 있다.")
  void withdraw_exactBalance() {
    // given
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(1L, userId, new Money(1000L));
    Long withdrawAmount = 1000L;

    when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

    // when
    WalletDto.WalletResponse result = walletService.withdraw(userId, withdrawAmount);

    // then
    assertThat(wallet.getBalance().getPrice()).isEqualTo(0L);
    assertThat(result.getBalance()).isEqualTo(0L);
    verify(walletRepository, times(1)).findByUserId(userId);
    // JPA 더티 체킹으로 자동 저장되므로 save() verify 불필요
  }
}
