package depth.finvibe.investment.modules.asset.domain;

import java.util.UUID;

import depth.finvibe.investment.shared.domain.TimeStampedBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Asset extends TimeStampedBaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    private Money totalPrice;

    private String name;

    private Long stockId;

    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @Setter
    private PortfolioGroup portfolioGroup;

    public void additionalBuy(Double amount, Money totalPrice) {
        this.amount += amount;
        this.totalPrice = this.totalPrice.plus(totalPrice);
    }

    public void partialSell(Double amount, Money totalPrice) {
        this.amount -= amount;
        this.totalPrice = this.totalPrice.minus(totalPrice);
    }

    public static Asset create(Double amount, Money totalPrice, String name, Long stockId, UUID userId) {
        return Asset.builder()
            .amount(amount)
            .totalPrice(totalPrice)
            .name(name)
            .stockId(stockId)
            .userId(userId)
            .build();
    }
}
