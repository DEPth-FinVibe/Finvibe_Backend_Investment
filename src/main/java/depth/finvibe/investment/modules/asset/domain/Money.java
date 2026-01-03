package depth.finvibe.investment.modules.asset.domain;

import depth.finvibe.investment.modules.asset.domain.error.AssetErrorCode;
import depth.finvibe.investment.shared.error.DomainException;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Money {

    private Double amount;

    private Currency currency;

    public static Money of(Double amount, Currency currency) {
        if(amount == null || currency == null) {
            throw new DomainException(AssetErrorCode.INVALID_MONEY_PARAMS);
        }
        if(amount < 0) {
            throw new DomainException(AssetErrorCode.NEGATIVE_MONEY_AMOUNT);
        }

        return new Money(amount, currency);
    }

    public Money plus(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(AssetErrorCode.CANNOT_ADD_DIFFERENT_CURRENCIES);
        }
        return new Money(this.amount + other.amount, this.currency);
    }

    public Money minus(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException(AssetErrorCode.CANNOT_SUBTRACT_DIFFERENT_CURRENCIES);
        }

        double resultAmount = this.amount - other.amount;

        if (resultAmount < 0) {
            throw new DomainException(AssetErrorCode.NEGATIVE_MONEY_AMOUNT);
        }

        return new Money(resultAmount, this.currency);
    }

}
