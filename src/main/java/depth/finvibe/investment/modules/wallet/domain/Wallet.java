package depth.finvibe.investment.modules.wallet.domain;

import depth.finvibe.investment.shared.domain.TimeStampedBaseEntity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Wallet extends TimeStampedBaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded
  private Money balance;

  public void deposit(Money amount) {
    this.balance = this.balance.plus(amount);
  }

  public void withdraw(Money amount) {
    this.balance = this.balance.minus(amount);
  }
}
