package depth.finvibe.investment.modules.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PortfolioGroupTest {

  @Test
  @DisplayName("PortfolioGroup.create로 그룹을 생성할 수 있다.")
  void createPortfolioGroup_success() {
    // given
    String name = "기본 포트폴리오";
    UUID userId = UUID.randomUUID();
    String iconCode = "ICON_CODE";

    // when
    PortfolioGroup portfolioGroup = PortfolioGroup.create(name, userId, iconCode);

    // then
    assertThat(portfolioGroup.getName()).isEqualTo(name);
    assertThat(portfolioGroup.getUserId()).isEqualTo(userId);
    assertThat(portfolioGroup.getIconCode()).isEqualTo(iconCode);
    assertThat(portfolioGroup.getAssets()).isNull();
    assertThat(portfolioGroup.getId()).isNull();
  }

  @Test
  @DisplayName("포트폴리오 그룹에 자산을 등록할 수 있다.")
  void registerAsset_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup portfolioGroup = PortfolioGroup.builder()
        .name("등록 테스트")
        .userId(userId)
        .iconCode("ICON")
        .assets(new ArrayList<>())
        .build();
    Asset asset = Asset.create(1.0, 5_000L, "자산", 1L, userId);

    // when
    portfolioGroup.registerAsset(asset);

    // then
    assertThat(portfolioGroup.getAssets()).containsExactly(asset);
    assertThat(asset.getPortfolioGroup()).isEqualTo(portfolioGroup);
  }

  @Test
  @DisplayName("포트폴리오 그룹에서 자산을 제거할 수 있다.")
  void unregisterAsset_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup portfolioGroup = PortfolioGroup.builder()
        .name("삭제 테스트")
        .userId(userId)
        .iconCode("ICON")
        .assets(new ArrayList<>())
        .build();
    Asset asset = Asset.create(1.0, 5_000L, "자산", 1L, userId);
    portfolioGroup.registerAsset(asset);

    // when
    portfolioGroup.unregisterAsset(asset);

    // then
    assertThat(portfolioGroup.getAssets()).isEmpty();
    assertThat(asset.getPortfolioGroup()).isNull();
  }
}
