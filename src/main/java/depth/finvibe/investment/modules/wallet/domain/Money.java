package depth.finvibe.investment.modules.wallet.domain;

import depth.finvibe.investment.modules.wallet.domain.error.WalletErrorCode;
import depth.finvibe.investment.shared.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@Getter
public class Money {

  @Column(name = "balance")
  private Long amount;

  public Money(Long amount) {
    if (amount == null || amount < 0) {
      throw new DomainException(WalletErrorCode.INVALID_MONEY_AMOUNT);
    }

    this.amount = amount;
  }

  public Money plus(Money other) {
    return new Money(this.amount + other.amount);
  }

  public Money minus(Money other) {
    if (this.amount < other.amount) {
      throw new DomainException(WalletErrorCode.INSUFFICIENT_BALANCE);
    }

    return new Money(this.amount - other.amount);
  }

}
