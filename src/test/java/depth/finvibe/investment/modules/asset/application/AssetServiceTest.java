package depth.finvibe.investment.modules.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.domain.Money;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.domain.error.AssetErrorCode;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.shared.error.DomainException;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

  @Mock
  PortfolioGroupRepository portfolioGroupRepository;

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
    when(portfolioGroupRepository.findByIdWithAssets(99L)).thenReturn(Optional.empty());

    PortfolioGroupDto.RegisterAssetRequest request = PortfolioGroupDto.RegisterAssetRequest.builder()
        .stockId(10L)
        .amount(BigDecimal.valueOf(1.0))
        .stockPrice(BigDecimal.valueOf(1_000))
        .name("자산")
        .currency(Currency.KRW)
        .build();

    // when / then
    assertThatThrownBy(() -> assetService.registerAsset(99L, request, UUID.randomUUID()))
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
    when(portfolioGroupRepository.findByIdWithAssets(99L)).thenReturn(Optional.empty());

    PortfolioGroupDto.UnregisterAssetRequest request = PortfolioGroupDto.UnregisterAssetRequest.builder()
        .stockId(10L)
        .amount(BigDecimal.valueOf(1.0))
        .stockPrice(BigDecimal.valueOf(1_000))
        .currency(Currency.KRW)
        .build();

    // when / then
    assertThatThrownBy(() -> assetService.unregisterAsset(99L, request, UUID.randomUUID()))
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
    when(portfolioGroupRepository.findById(1L)).thenReturn(Optional.of(existing));

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
}
