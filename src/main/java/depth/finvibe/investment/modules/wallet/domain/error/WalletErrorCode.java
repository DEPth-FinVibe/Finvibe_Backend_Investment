package depth.finvibe.investment.modules.wallet.domain.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 지갑(Wallet) 모듈 내에서 발생할 수 있는 비즈니스 에러 정의입니다.
 * 
 * <p>
 * {@link DomainErrorCode}를 구현하여 지갑 도메인의 고유한 에러 상황을 명세합니다.
 * 각 상수는 에러 식별 코드와 다국어 메시지 키를 포함합니다.
 * </p>
 */
@AllArgsConstructor
@Getter
public enum WalletErrorCode implements DomainErrorCode {
    INVALID_USER_ID("WALLET_INVALID_USER_ID", "error.wallet.invalid_user_id"),
  WALLET_NOT_FOUND("WALLET_NOT_FOUND", "error.wallet.not_found"),
  INSUFFICIENT_BALANCE("WALLET_INSUFFICIENT_BALANCE", "error.wallet.insufficient_balance"),
  INVALID_MONEY_AMOUNT("WALLET_INVALID_MONEY_AMOUNT", "error.wallet.invalid_money_amount");

  private final String code;
  private final String messageKey;
}
