package depth.finvibe.investment.modules.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    Asset asset = Asset.create(BigDecimal.valueOf(1.0), BigDecimal.valueOf(5_000), Currency.KRW, "자산", 1L, userId);

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
    Asset asset = Asset.create(BigDecimal.valueOf(1.0), BigDecimal.valueOf(5_000), Currency.KRW, "자산", 1L, userId);
    portfolioGroup.register(asset, userId);

    // when
    portfolioGroup.unregister(asset.getStockId(), asset.getAmount(), asset.getTotalPrice(), userId);

    // then
    assertThat(portfolioGroup.getAssets()).isEmpty();
    assertThat(asset.getPortfolioGroup()).isNull();
  }

  @Test
  @DisplayName("매도 후 수량이 0에 매우 가까우면(미세 잔량) 자산을 삭제한다.")
  void unregisterAsset_effectivelyZeroAmount_removesAsset() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup portfolioGroup = PortfolioGroup.builder()
        .name("미세 잔량 삭제 테스트")
        .userId(userId)
        .iconCode("ICON")
        .assets(new ArrayList<>())
        .build();
    Asset asset = Asset.create(new BigDecimal("1.0"), BigDecimal.valueOf(5_000), Currency.KRW, "자산", 1L, userId);
    portfolioGroup.register(asset, userId);

    // when: 1.0 - 0.9999995 = 0.0000005 (threshold 0.000001 이하)
    portfolioGroup.unregister(asset.getStockId(), new BigDecimal("0.9999995"), asset.getTotalPrice(), userId);

    // then
    assertThat(portfolioGroup.getAssets()).isEmpty();
    assertThat(asset.getPortfolioGroup()).isNull();
  }

  @Test
  @DisplayName("매도 후 수량이 임계값보다 크면 자산을 삭제하지 않는다.")
  void unregisterAsset_aboveZeroThreshold_keepsAsset() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup portfolioGroup = PortfolioGroup.builder()
        .name("임계값 초과 유지 테스트")
        .userId(userId)
        .iconCode("ICON")
        .assets(new ArrayList<>())
        .build();
    Asset asset = Asset.create(new BigDecimal("1.0"), BigDecimal.valueOf(5_000), Currency.KRW, "자산", 1L, userId);
    portfolioGroup.register(asset, userId);

    // when: 1.0 - 0.999998 = 0.000002 (threshold 초과)
    portfolioGroup.unregister(asset.getStockId(), new BigDecimal("0.999998"), Money.of(BigDecimal.ZERO, Currency.KRW), userId);

    // then
    assertThat(portfolioGroup.getAssets()).hasSize(1);
    assertThat(portfolioGroup.getAssets().get(0).getPortfolioGroup()).isEqualTo(portfolioGroup);
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
    Asset asset = Asset.create(BigDecimal.valueOf(1.0), BigDecimal.valueOf(5_000), Currency.KRW, "자산", 1L, ownerId);

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
    Asset existing = Asset.create(BigDecimal.valueOf(1.0), BigDecimal.valueOf(5_000), Currency.KRW, "자산", 1L, userId);
    portfolioGroup.register(existing, userId);

    Asset additional = Asset.create(BigDecimal.valueOf(2.0), BigDecimal.valueOf(5_000), Currency.KRW, "자산", 1L, userId);

    // when
    portfolioGroup.register(additional, userId);

    // then
    assertThat(portfolioGroup.getAssets()).hasSize(1);
    Asset merged = portfolioGroup.getAssets().get(0);
    assertThat(merged.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3.0d));
    assertThat(merged.getTotalPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(15_000d));
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
    assertThatThrownBy(() -> portfolioGroup.unregister(99L, BigDecimal.valueOf(1.0), Money.of(1_000d, Currency.KRW), userId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.CANNOT_SELL_NON_EXISTENT_ASSET));
  }

  @Test
  @DisplayName("기본 포트폴리오 그룹을 생성할 수 있다.")
  void createDefault_success() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    PortfolioGroup defaultGroup = PortfolioGroup.createDefault(userId);

    // then
    assertThat(defaultGroup.getName()).isEqualTo("기본 포트폴리오");
    assertThat(defaultGroup.getUserId()).isEqualTo(userId);
    assertThat(defaultGroup.getIsDefault()).isTrue();
  }

  @Test
  @DisplayName("포트폴리오 그룹 정보를 수정할 수 있다.")
  void patch_success() {
    // given
    PortfolioGroup group = PortfolioGroup.builder()
        .name("기존 이름")
        .iconCode("OLD")
        .isDefault(false)
        .build();

    // when
    group.patch("새 이름", "NEW");

    // then
    assertThat(group.getName()).isEqualTo("새 이름");
    assertThat(group.getIconCode()).isEqualTo("NEW");
  }

  @Test
  @DisplayName("기본 포트폴리오 그룹은 수정할 수 없다.")
  void patch_defaultGroup_fail() {
    // given
    PortfolioGroup defaultGroup = PortfolioGroup.builder()
        .isDefault(true)
        .build();

    // when / then
    assertThatThrownBy(() -> defaultGroup.patch("새 이름", "NEW"))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.CANNOT_MODIFY_DEFAULT_PORTFOLIO_GROUP));
  }

  @Test
  @DisplayName("자산을 다른 그룹으로 이전할 수 있다. (중복 종목 병합)")
  void transferAssetsTo_merge_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup source = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();
    Asset asset1 = Asset.builder()
        .id(101L)
        .amount(BigDecimal.valueOf(10))
        .totalPrice(Money.of(BigDecimal.valueOf(1_000), Currency.KRW))
        .name("자산1")
        .stockId(1L)
        .userId(userId)
        .build();
    source.register(asset1, userId);

    PortfolioGroup target = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();
    Asset asset2 = Asset.builder()
        .id(102L)
        .amount(BigDecimal.valueOf(5))
        .totalPrice(Money.of(BigDecimal.valueOf(500), Currency.KRW))
        .name("자산1")
        .stockId(1L)
        .userId(userId)
        .build();
    target.register(asset2, userId);

    // when
    var removedAssetIds = source.transferAssetsTo(target);

    // then
    assertThat(source.getAssets()).isEmpty();
    assertThat(removedAssetIds).containsExactly(101L);
    assertThat(target.getAssets()).hasSize(1);
    assertThat(target.getAssets().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(15));
    assertThat(target.getAssets().get(0).getTotalPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
  }

  @Test
  @DisplayName("자산을 다른 그룹으로 이전할 수 있다. (새 종목 추가)")
  void transferAssetsTo_move_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup source = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();
    Asset asset1 = Asset.create(BigDecimal.valueOf(10), BigDecimal.valueOf(1000), Currency.KRW, "자산1", 1L, userId);
    source.register(asset1, userId);

    PortfolioGroup target = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();

    // when
    var removedAssetIds = source.transferAssetsTo(target);

    // then
    assertThat(source.getAssets()).isEmpty();
    assertThat(removedAssetIds).isEmpty();
    assertThat(target.getAssets()).hasSize(1);
    assertThat(target.getAssets().get(0).getName()).isEqualTo("자산1");
    assertThat(target.getAssets().get(0).getPortfolioGroup()).isEqualTo(target);
  }

  @Test
  @DisplayName("특정 종목 자산을 다른 그룹으로 전량 이동할 수 있다.")
  void transferAssetTo_success() {
    UUID userId = UUID.randomUUID();
    PortfolioGroup source = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();
    Asset asset = Asset.builder()
        .id(11L)
        .amount(BigDecimal.valueOf(10))
        .totalPrice(Money.of(BigDecimal.valueOf(10_000), Currency.KRW))
        .name("자산1")
        .stockId(1L)
        .userId(userId)
        .build();
    source.register(asset, userId);

    PortfolioGroup target = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();

    var result = source.transferAssetTo(11L, target, userId);

    assertThat(source.getAssets()).isEmpty();
    assertThat(target.getAssets()).hasSize(1);
    assertThat(result).isEmpty();
    assertThat(target.getAssets().get(0).getId()).isEqualTo(11L);
    assertThat(target.getAssets().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10));
    assertThat(target.getAssets().get(0).getPortfolioGroup()).isEqualTo(target);
  }

  @Test
  @DisplayName("특정 종목 자산 이동 시 대상 그룹에 동일 종목이 있으면 병합된다.")
  void transferAssetTo_merge_success() {
    UUID userId = UUID.randomUUID();
    PortfolioGroup source = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();
    Asset sourceAsset = Asset.builder()
        .id(21L)
        .amount(BigDecimal.valueOf(3))
        .totalPrice(Money.of(BigDecimal.valueOf(3_000), Currency.KRW))
        .name("자산1")
        .stockId(1L)
        .userId(userId)
        .build();
    source.register(sourceAsset, userId);

    PortfolioGroup target = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();
    Asset targetAsset = Asset.builder()
        .id(22L)
        .amount(BigDecimal.valueOf(2))
        .totalPrice(Money.of(BigDecimal.valueOf(2_000), Currency.KRW))
        .name("자산1")
        .stockId(1L)
        .userId(userId)
        .build();
    target.register(targetAsset, userId);

    var result = source.transferAssetTo(21L, target, userId);

    assertThat(source.getAssets()).isEmpty();
    assertThat(target.getAssets()).hasSize(1);
    assertThat(result).contains(21L);
    assertThat(target.getAssets().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5));
    assertThat(target.getAssets().get(0).getTotalPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5_000));
  }

  @Test
  @DisplayName("특정 종목 자산 이동 시 소유자가 아니면 예외가 발생한다.")
  void transferAssetTo_notOwner_fail() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    PortfolioGroup source = PortfolioGroup.builder()
        .userId(ownerId)
        .assets(new ArrayList<>())
        .build();
    Asset asset = Asset.builder()
        .id(12L)
        .amount(BigDecimal.valueOf(3))
        .totalPrice(Money.of(BigDecimal.valueOf(3_000), Currency.KRW))
        .name("자산1")
        .stockId(1L)
        .userId(ownerId)
        .build();
    source.register(asset, ownerId);
    PortfolioGroup target = PortfolioGroup.builder()
        .userId(ownerId)
        .assets(new ArrayList<>())
        .build();

    assertThatThrownBy(() -> source.transferAssetTo(12L, target, otherId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.ONLY_OWNER_CAN_TRANSFER_ASSET));
  }

  @Test
  @DisplayName("특정 종목 자산 이동 시 원본 그룹에 종목이 없으면 예외가 발생한다.")
  void transferAssetTo_assetNotFound_fail() {
    UUID userId = UUID.randomUUID();
    PortfolioGroup source = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();
    PortfolioGroup target = PortfolioGroup.builder()
        .userId(userId)
        .assets(new ArrayList<>())
        .build();

    assertThatThrownBy(() -> source.transferAssetTo(99L, target, userId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.ASSET_NOT_FOUND));
  }

  @Test
  @DisplayName("삭제 가능 여부 확인 - 성공")
  void ensureDeletable_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup group = PortfolioGroup.builder()
        .userId(userId)
        .isDefault(false)
        .build();

    // when / then (no exception)
    group.ensureDeletable(userId);
  }

  @Test
  @DisplayName("삭제 가능 여부 확인 - 기본 그룹 실패")
  void ensureDeletable_defaultGroup_fail() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup group = PortfolioGroup.builder()
        .userId(userId)
        .isDefault(true)
        .build();

    // when / then
    assertThatThrownBy(() -> group.ensureDeletable(userId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.CANNOT_DELETE_DEFAULT_PORTFOLIO_GROUP));
  }

  @Test
  @DisplayName("삭제 가능 여부 확인 - 소유자 아님 실패")
  void ensureDeletable_notOwner_fail() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    PortfolioGroup group = PortfolioGroup.builder()
        .userId(ownerId)
        .isDefault(false)
        .build();

    // when / then
    assertThatThrownBy(() -> group.ensureDeletable(otherId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.ONLY_OWNER_CAN_DELETE_PORTFOLIO_GROUP));
  }
}
