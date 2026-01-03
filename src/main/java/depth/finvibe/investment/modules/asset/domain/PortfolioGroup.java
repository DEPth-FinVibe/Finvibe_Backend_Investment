package depth.finvibe.investment.modules.asset.domain;

import java.util.UUID;

import depth.finvibe.investment.shared.domain.TimeStampedBaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import java.util.List;

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

    @OneToMany(mappedBy = "portfolioGroup", fetch = FetchType.LAZY)
    private List<Asset> assets;

    public static PortfolioGroup create(String name, UUID userId, String iconCode) {
        return PortfolioGroup.builder()
            .name(name)
            .userId(userId)
            .iconCode(iconCode)
            .build();
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
