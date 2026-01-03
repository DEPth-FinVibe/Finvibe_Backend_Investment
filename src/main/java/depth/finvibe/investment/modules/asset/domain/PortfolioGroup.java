package depth.finvibe.investment.modules.asset.domain;

import java.util.Optional;
import java.util.UUID;

import depth.finvibe.investment.modules.asset.domain.error.AssetErrorCode;
import depth.finvibe.investment.shared.domain.TimeStampedBaseEntity;
import depth.finvibe.investment.shared.error.DomainException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioGroup extends TimeStampedBaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private UUID userId;

    private String iconCode;

    @Builder.Default
    private Boolean isDefault = false;

    @OneToMany(mappedBy = "portfolioGroup", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Asset> assets = new ArrayList<>();

    public static PortfolioGroup create(String name, UUID userId, String iconCode) {
        if(name.isBlank() || userId == null) {
            throw new DomainException(AssetErrorCode.INVALID_PORTFOLIO_GROUP_PARAMS);
        }

        return PortfolioGroup.builder()
            .name(name)
            .userId(userId)
            .iconCode(iconCode)
            .build();
    }

    public void patch(String name, String iconCode) {
        if(name != null) {
            this.name = name;
        }
        if(iconCode != null) {
            this.iconCode = iconCode;
        }
    }

    public void register(Asset asset, UUID requesterId) {
        if(!this.userId.equals(requesterId)) {
            throw new DomainException(AssetErrorCode.ONLY_OWNER_CAN_REGISTER_ASSET);
        }

        Optional<Asset> foundAsset = assets.stream()
                .filter(it -> it.getStockId().equals(asset.getStockId()))
                .findFirst();

        if(foundAsset.isPresent()) {
            foundAsset.get().additionalBuy(asset.getAmount(), asset.getTotalPrice());
        } else {
            this.assets.add(asset); // cascade 옵션으로 인해 PortfolioGroup이 저장될 때 Asset도 함께 저장
            asset.setPortfolioGroup(this);
        }
    }

    public void unregister(Long stockId, Double amount, Money paidMoney) {
        Optional<Asset> foundAsset = assets.stream()
                .filter(it -> it.getStockId().equals(stockId))
                .findFirst();

        if(foundAsset.isEmpty()) {
            throw new DomainException(AssetErrorCode.CANNOT_SELL_NON_EXISTENT_ASSET);
        }

        foundAsset.get().partialSell(amount, paidMoney);

        if(foundAsset.get().getAmount() == 0) {
            this.assets.remove(foundAsset.get()); // orphanRemoval을 사용해 0주가 된 자산을 자동으로 삭제
            foundAsset.get().setPortfolioGroup(null);
        }
    }
}
