package depth.finvibe.investment.modules.asset.domain;

import java.util.UUID;

import depth.finvibe.investment.modules.asset.domain.error.AssetErrorCode;
import depth.finvibe.investment.shared.domain.TimeStampedBaseEntity;
import depth.finvibe.investment.shared.error.DomainException;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;
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

    @OneToMany(mappedBy = "portfolioGroup", fetch = FetchType.LAZY)
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

    public void registerAsset(Asset asset) {
        this.assets.add(asset);
        asset.setPortfolioGroup(this);
    }

    public void unregisterAsset(Asset asset) {
        this.assets.remove(asset);
        asset.setPortfolioGroup(null);
    }
}
