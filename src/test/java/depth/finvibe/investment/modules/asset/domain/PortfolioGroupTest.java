package depth.finvibe.investment.modules.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import depth.finvibe.investment.modules.asset.domain.Money;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.domain.error.AssetErrorCode;
import depth.finvibe.investment.shared.error.DomainException;

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
    assertThat(portfolioGroup.getAssets()).isEmpty();
    assertThat(portfolioGroup.getId()).isNull();
  }

  @Test
  @DisplayName("그룹 생성 시 이름이 비어 있으면 예외가 발생한다.")
  void createPortfolioGroup_blankName_fail() {
    // given
    UUID userId = UUID.randomUUID();

    // when / then
    assertThatThrownBy(() -> PortfolioGroup.create("   ", userId, "ICON"))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.INVALID_PORTFOLIO_GROUP_PARAMS));
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
    Asset asset = Asset.create(1.0, Money.of(5_000d, Currency.KRW), "자산", 1L, userId);

    // when
    portfolioGroup.register(asset, userId);

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
    Asset asset = Asset.create(1.0, Money.of(5_000d, Currency.KRW), "자산", 1L, userId);
    portfolioGroup.register(asset, userId);

    // when
    portfolioGroup.unregister(asset.getStockId(), asset.getAmount(), asset.getTotalPrice(), userId);

    // then
    assertThat(portfolioGroup.getAssets()).isEmpty();
    assertThat(asset.getPortfolioGroup()).isNull();
  }

  @Test
  @DisplayName("소유자가 아닌 사용자가 등록하면 예외가 발생한다.")
  void registerAsset_notOwner_fail() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID otherUser = UUID.randomUUID();
    PortfolioGroup portfolioGroup = PortfolioGroup.builder()
        .name("등록 실패 테스트")
        .userId(ownerId)
        .iconCode("ICON")
        .assets(new ArrayList<>())
        .build();
    Asset asset = Asset.create(1.0, Money.of(5_000d, Currency.KRW), "자산", 1L, ownerId);

    // when / then
    assertThatThrownBy(() -> portfolioGroup.register(asset, otherUser))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.ONLY_OWNER_CAN_REGISTER_ASSET));
  }

  @Test
  @DisplayName("같은 종목을 다시 등록하면 수량과 총액이 누적된다.")
  void registerAsset_duplicateStock_merge() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup portfolioGroup = PortfolioGroup.builder()
        .name("누적 테스트")
        .userId(userId)
        .iconCode("ICON")
        .assets(new ArrayList<>())
        .build();
    Asset existing = Asset.create(1.0, Money.of(5_000d, Currency.KRW), "자산", 1L, userId);
    existing.setPortfolioGroup(portfolioGroup);
    portfolioGroup.getAssets().add(existing);

    Asset additional = Asset.create(2.0, Money.of(10_000d, Currency.KRW), "자산", 1L, userId);

    // when
    portfolioGroup.register(additional, userId);

    // then
    assertThat(portfolioGroup.getAssets()).hasSize(1);
    Asset merged = portfolioGroup.getAssets().get(0);
    assertThat(merged.getAmount()).isEqualTo(3.0d);
    assertThat(merged.getTotalPrice().getAmount()).isEqualTo(15_000d);
    assertThat(merged.getTotalPrice().getCurrency()).isEqualTo(Currency.KRW);
  }

  @Test
  @DisplayName("보유하지 않은 종목을 매도하려 하면 예외가 발생한다.")
  void unregisterAsset_notExist_fail() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup portfolioGroup = PortfolioGroup.builder()
        .name("삭제 실패 테스트")
        .userId(userId)
        .iconCode("ICON")
        .assets(new ArrayList<>())
        .build();

    // when / then
    assertThatThrownBy(() -> portfolioGroup.unregister(99L, 1.0, Money.of(1_000d, Currency.KRW), userId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.CANNOT_SELL_NON_EXISTENT_ASSET));
  }
}
