package depth.finvibe.investment.modules.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.application.port.out.TopHoldingStockCacheRepository;
import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.domain.Money;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.domain.PortfolioValuation;
import depth.finvibe.investment.modules.asset.domain.error.AssetErrorCode;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.modules.asset.dto.TopHoldingStockDto;
import depth.finvibe.investment.shared.application.port.out.GamificationEventProducer;
import depth.finvibe.investment.shared.error.DomainException;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

  @Mock
  PortfolioGroupRepository portfolioGroupRepository;

  @Mock
  GamificationEventProducer gamificationEventProducer;

  @Mock
  TopHoldingStockCacheRepository topHoldingStockCacheRepository;

  @InjectMocks
  AssetService assetService;

  @Test
  @DisplayName("자산 등록 시 포트폴리오를 찾아 등록하고 금액을 계산한다.")
  void registerAsset_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup existing = PortfolioGroup.builder()
        .name("기본")
        .userId(userId)
        .iconCode("ICON")
        .assets(new ArrayList<>())
        .build();
    when(portfolioGroupRepository.findByIdWithAssets(1L)).thenReturn(Optional.of(existing));
    when(portfolioGroupRepository.findAllByUserIdWithAssets(userId)).thenReturn(List.of(existing));

    PortfolioGroupDto.RegisterAssetRequest request = PortfolioGroupDto.RegisterAssetRequest.builder()
        .stockId(10L)
        .amount(BigDecimal.valueOf(2.0))
        .stockPrice(BigDecimal.valueOf(5_000))
        .name("자산")
        .currency(Currency.KRW)
        .build();

    // when
    assetService.registerAsset(1L, request, userId);

    // then
    assertThat(existing.getAssets()).hasSize(1);
    Asset registered = existing.getAssets().get(0);
    assertThat(registered.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
    assertThat(registered.getTotalPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
    assertThat(registered.getTotalPrice().getCurrency()).isEqualTo(Currency.KRW);
    assertThat(registered.getPortfolioGroup()).isEqualTo(existing);
  }

  @Test
  @DisplayName("없는 포트폴리오에 등록 시 예외를 던진다.")
  void registerAsset_notFound_fail() {
    // given
    UUID userId = UUID.randomUUID();
    when(portfolioGroupRepository.findByIdWithAssets(99L)).thenReturn(Optional.empty());
    when(portfolioGroupRepository.findAllByUserIdWithAssets(userId)).thenReturn(List.of());

    PortfolioGroupDto.RegisterAssetRequest request = PortfolioGroupDto.RegisterAssetRequest.builder()
        .stockId(10L)
        .amount(BigDecimal.valueOf(1.0))
        .stockPrice(BigDecimal.valueOf(1_000))
        .name("자산")
        .currency(Currency.KRW)
        .build();

    // when / then
    assertThatThrownBy(() -> assetService.registerAsset(99L, request, userId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));
  }

  @Test
  @DisplayName("자산 해제 시 포트폴리오를 찾아 매도 요청을 전달한다.")
  void unregisterAsset_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup portfolioGroup = org.mockito.Mockito.mock(PortfolioGroup.class);
    when(portfolioGroupRepository.findByIdWithAssets(1L)).thenReturn(Optional.of(portfolioGroup));
    when(portfolioGroupRepository.findAllByUserIdWithAssets(userId)).thenReturn(List.of());

    PortfolioGroupDto.UnregisterAssetRequest request = PortfolioGroupDto.UnregisterAssetRequest.builder()
        .stockId(5L)
        .amount(BigDecimal.valueOf(1.5))
        .stockPrice(BigDecimal.valueOf(3_000))
        .currency(Currency.KRW)
        .build();

    ArgumentCaptor<Money> moneyCaptor = ArgumentCaptor.forClass(Money.class);

    // when
    assetService.unregisterAsset(1L, request, userId);

    // then
    verify(portfolioGroup).unregister(eq(5L), eq(BigDecimal.valueOf(1.5)), moneyCaptor.capture(), eq(userId));
    Money paidMoney = moneyCaptor.getValue();
    assertThat(paidMoney.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3_000));
    assertThat(paidMoney.getCurrency()).isEqualTo(Currency.KRW);
  }

  @Test
  @DisplayName("없는 포트폴리오에서 자산 해제 시 예외를 던진다.")
  void unregisterAsset_notFound_fail() {
    // given
    UUID userId = UUID.randomUUID();
    when(portfolioGroupRepository.findByIdWithAssets(99L)).thenReturn(Optional.empty());
    when(portfolioGroupRepository.findAllByUserIdWithAssets(userId)).thenReturn(List.of());

    PortfolioGroupDto.UnregisterAssetRequest request = PortfolioGroupDto.UnregisterAssetRequest.builder()
        .stockId(10L)
        .amount(BigDecimal.valueOf(1.0))
        .stockPrice(BigDecimal.valueOf(1_000))
        .currency(Currency.KRW)
        .build();

    // when / then
    assertThatThrownBy(() -> assetService.unregisterAsset(99L, request, userId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));
  }

  @Test
  @DisplayName("포트폴리오 그룹을 생성하면 리포지토리에 저장된다.")
  void createPortfolioGroup_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroupDto.CreatePortfolioGroupRequest request = PortfolioGroupDto.CreatePortfolioGroupRequest.builder()
        .name("새 그룹")
        .iconCode("ICON")
        .build();

    ArgumentCaptor<PortfolioGroup> captor = ArgumentCaptor.forClass(PortfolioGroup.class);
    when(portfolioGroupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    assetService.createPortfolioGroup(request, userId);

    // then
    verify(portfolioGroupRepository).save(captor.capture());
    PortfolioGroup saved = captor.getValue();
    assertThat(saved.getName()).isEqualTo("새 그룹");
    assertThat(saved.getIconCode()).isEqualTo("ICON");
    assertThat(saved.getUserId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("포트폴리오 그룹을 수정하면 이름과 아이콘이 반영된다.")
  void updatePortfolioGroup_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup existing = PortfolioGroup.builder()
        .name("이전 이름")
        .iconCode("OLD_ICON")
        .userId(userId)
        .assets(new ArrayList<>())
        .build();
    when(portfolioGroupRepository.findByIdWithAssets(1L)).thenReturn(Optional.of(existing));

    PortfolioGroupDto.UpdatePortfolioGroupRequest request = PortfolioGroupDto.UpdatePortfolioGroupRequest.builder()
        .name("새 이름")
        .iconCode("NEW_ICON")
        .build();

    // when
    assetService.updatePortfolioGroup(1L, request, userId);

    // then
    assertThat(existing.getName()).isEqualTo("새 이름");
    assertThat(existing.getIconCode()).isEqualTo("NEW_ICON");
  }

  @Test
  @DisplayName("존재하지 않는 포트폴리오 그룹 수정 시 예외를 던진다.")
  void updatePortfolioGroup_notFound_fail() {
    // given
    when(portfolioGroupRepository.findByIdWithAssets(99L)).thenReturn(Optional.empty());

    PortfolioGroupDto.UpdatePortfolioGroupRequest request = PortfolioGroupDto.UpdatePortfolioGroupRequest.builder()
        .name("새 이름")
        .iconCode("NEW_ICON")
        .build();

    // when / then
    assertThatThrownBy(() -> assetService.updatePortfolioGroup(99L, request, UUID.randomUUID()))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.PORTFOLIO_GROUP_NOT_FOUND));
  }

  @Test
  @DisplayName("포트폴리오의 자산 목록을 조회한다.")
  void getAssetsByPortfolio_success() {
    // given
    UUID userId = UUID.randomUUID();
    Asset asset = Asset.create(BigDecimal.valueOf(10), BigDecimal.valueOf(10000d), Currency.KRW, "삼성전자", 1L, userId);
    PortfolioGroup portfolioGroup = PortfolioGroup.builder()
        .id(1L)
        .name("주식")
        .userId(userId)
        .assets(new ArrayList<>())
        .build();
    portfolioGroup.register(asset, userId);

    when(portfolioGroupRepository.findByIdWithAssets(1L)).thenReturn(Optional.of(portfolioGroup));

    // when
    List<PortfolioGroupDto.AssetResponse> results = assetService.getAssetsByPortfolio(1L, userId);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getName()).isEqualTo("삼성전자");
    assertThat(results.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10));
  }

  @Test
  @DisplayName("다른 사용자의 자산 목록 조회 시 예외를 던진다.")
  void getAssetsByPortfolio_notOwner_fail() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    PortfolioGroup portfolioGroup = PortfolioGroup.builder()
        .id(1L)
        .name("주식")
        .userId(ownerId)
        .build();

    when(portfolioGroupRepository.findByIdWithAssets(1L)).thenReturn(Optional.of(portfolioGroup));

    // when / then
    assertThatThrownBy(() -> assetService.getAssetsByPortfolio(1L, requesterId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.ONLY_OWNER_CAN_VIEW_ASSETS));
  }

  @Test
  @DisplayName("사용자의 포트폴리오 목록을 조회하면 투자원금, 현재가, 수익률이 포함된다.")
  void getPortfoliosByUser_success() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioValuation valuation = PortfolioValuation.builder()
        .totalCurrentValue(BigDecimal.valueOf(1_100_000))
        .totalProfitLoss(BigDecimal.valueOf(100_000))
        .totalReturnRate(BigDecimal.valueOf(10.00))
        .build();
    PortfolioGroup group = PortfolioGroup.builder()
        .id(1L).name("그룹1").iconCode("ICON1").userId(userId)
        .valuation(valuation)
        .build();
    when(portfolioGroupRepository.findAllByUserId(userId)).thenReturn(List.of(group));

    // when
    List<PortfolioGroupDto.PortfolioGroupResponse> results = assetService.getPortfoliosByUser(userId);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getName()).isEqualTo("그룹1");
    assertThat(results.get(0).getIconCode()).isEqualTo("ICON1");
    assertThat(results.get(0).getTotalCurrentValue()).isEqualByComparingTo(BigDecimal.valueOf(1_100_000));
    assertThat(results.get(0).getTotalPurchaseAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000));
    assertThat(results.get(0).getTotalReturnRate()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
  }

  @Test
  @DisplayName("포트폴리오의 valuation이 null이면 투자원금, 현재가, 수익률이 0으로 반환된다.")
  void getPortfoliosByUser_nullValuation_returnsZeros() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup group = PortfolioGroup.builder()
        .id(1L).name("빈 그룹").iconCode("ICON1").userId(userId)
        .build();
    when(portfolioGroupRepository.findAllByUserId(userId)).thenReturn(List.of(group));

    // when
    List<PortfolioGroupDto.PortfolioGroupResponse> results = assetService.getPortfoliosByUser(userId);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getTotalCurrentValue()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(results.get(0).getTotalPurchaseAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(results.get(0).getTotalReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("포트폴리오 그룹을 삭제하면 자산이 기본 그룹으로 이전되고 삭제된다.")
  void deletePortfolioGroup_success() {
    // given
    UUID userId = UUID.randomUUID();
    Asset asset = Asset.create(BigDecimal.valueOf(10), BigDecimal.valueOf(10000d), Currency.KRW, "삼성전자", 1L, userId);
    PortfolioGroup existing = PortfolioGroup.builder()
        .id(1L)
        .name("삭제할 그룹")
        .userId(userId)
        .isDefault(false)
        .assets(new ArrayList<>())
        .build();
    existing.register(asset, userId);

    PortfolioGroup defaultGroup = PortfolioGroup.builder()
        .id(2L)
        .name("기본 그룹")
        .userId(userId)
        .isDefault(true)
        .assets(new ArrayList<>())
        .build();

    when(portfolioGroupRepository.findByIdWithAssets(1L)).thenReturn(Optional.of(existing));
    when(portfolioGroupRepository.findDefaultByUserId(userId)).thenReturn(Optional.of(defaultGroup));

    // when
    assetService.deletePortfolioGroup(1L, userId);

    // then
    assertThat(defaultGroup.getAssets()).hasSize(1);
    assertThat(defaultGroup.getAssets().get(0).getName()).isEqualTo("삼성전자");
    verify(portfolioGroupRepository).delete(existing);
  }

  @Test
  @DisplayName("기본 포트폴리오 그룹 삭제 시도 시 예외를 던진다.")
  void deletePortfolioGroup_defaultGroup_fail() {
    // given
    UUID userId = UUID.randomUUID();
    PortfolioGroup defaultGroup = PortfolioGroup.builder()
        .id(1L)
        .isDefault(true)
        .userId(userId)
        .build();
    when(portfolioGroupRepository.findByIdWithAssets(1L)).thenReturn(Optional.of(defaultGroup));

    // when / then
    assertThatThrownBy(() -> assetService.deletePortfolioGroup(1L, userId))
        .isInstanceOf(DomainException.class)
        .satisfies(ex -> assertThat(((DomainException) ex).getErrorCode()).isEqualTo(AssetErrorCode.CANNOT_DELETE_DEFAULT_PORTFOLIO_GROUP));
  }

  @Test
  @DisplayName("상위 보유 종목 캐시 미스 시 DB 집계를 조회하고 캐시에 저장한다.")
  void getTopHoldingStocks_cacheMiss_fetchFromSourceAndCache() {
    // given
    UUID userId = UUID.randomUUID();
    TopHoldingStockDto.TopHoldingStockResponse item = TopHoldingStockDto.TopHoldingStockResponse.builder()
        .stockId(101L)
        .name("애플")
        .totalAmount(BigDecimal.valueOf(12.5))
        .build();
    when(topHoldingStockCacheRepository.find(userId)).thenReturn(Optional.empty());
    when(portfolioGroupRepository.findTopHoldingStocks(userId, 100)).thenReturn(List.of(item));

    // when
    TopHoldingStockDto.TopHoldingStockListResponse result = assetService.getTopHoldingStocks(userId);

    // then
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getStockId()).isEqualTo(101L);
    verify(portfolioGroupRepository).findTopHoldingStocks(userId, 100);
    verify(topHoldingStockCacheRepository).save(userId, result);
  }

  @Test
  @DisplayName("상위 보유 종목 캐시 히트 시 DB를 조회하지 않는다.")
  void getTopHoldingStocks_cacheHit_returnCacheOnly() {
    // given
    UUID userId = UUID.randomUUID();
    TopHoldingStockDto.TopHoldingStockResponse item = TopHoldingStockDto.TopHoldingStockResponse.builder()
        .stockId(101L)
        .name("애플")
        .totalAmount(BigDecimal.valueOf(12.5))
        .build();
    TopHoldingStockDto.TopHoldingStockListResponse cached = TopHoldingStockDto.TopHoldingStockListResponse.builder()
        .totalElements(1)
        .items(List.of(item))
        .build();
    when(topHoldingStockCacheRepository.find(userId)).thenReturn(Optional.of(cached));

    // when
    TopHoldingStockDto.TopHoldingStockListResponse result = assetService.getTopHoldingStocks(userId);

    // then
    assertThat(result).isEqualTo(cached);
    verify(portfolioGroupRepository, never()).findTopHoldingStocks(any(UUID.class), anyInt());
  }
}
