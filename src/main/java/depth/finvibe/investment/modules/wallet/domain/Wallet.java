package depth.finvibe.investment.modules.wallet.domain;

import depth.finvibe.investment.shared.domain.TimeStampedBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Wallet extends TimeStampedBaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private UUID userId;

  @Embedded
  private Money balance;

  public void deposit(Money amount) {
    this.balance = this.balance.plus(amount);
  }

  public void withdraw(Money amount) {
    this.balance = this.balance.minus(amount);
  }

  public static Wallet create(UUID userId) {
    return new Wallet(null, userId, new Money(1000000L));
  }
}
