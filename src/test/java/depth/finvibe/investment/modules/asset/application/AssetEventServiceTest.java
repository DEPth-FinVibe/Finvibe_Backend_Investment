package depth.finvibe.investment.modules.asset.application;

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

import depth.finvibe.investment.modules.asset.application.event.AssetTransferredEvent;
import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.ProfitCalculationUseCase;
import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.shared.dto.SignUpEvent;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetEventServiceTest {

    @Mock
    private AssetCommandUseCase commandUseCase;

    @Mock
    private ProfitCalculationUseCase profitCalculationService;

    @Mock
    private PortfolioGroupRepository portfolioGroupRepository;

    @InjectMocks
    private AssetEventService assetEventService;

    @Test
    @DisplayName("BUY 이벤트 수신 시 registerAsset이 올바른 데이터로 호출되어야 한다")
    void handleTradeExecutedEvent_Buy() {
        // given
        Long portfolioId = 10L;
        UUID userId = UUID.randomUUID();

        TradeExecutedEvent event = TradeExecutedEvent.builder()
                .tradeId(1L)
                .userId(userId.toString())
                .type("BUY")
                .amount(BigDecimal.valueOf(5))
                .price(70000L)
                .stockId(101L)
                .name("삼성전자")
                .currency("KRW")
                .portfolioId(portfolioId)
                .build();

        // when
        assetEventService.handleTradeExecutedEvent(event);

        // then
        ArgumentCaptor<PortfolioGroupDto.RegisterAssetRequest> captor =
                ArgumentCaptor.forClass(PortfolioGroupDto.RegisterAssetRequest.class);

        verify(commandUseCase).registerAsset(eq(portfolioId), captor.capture(), eq(userId));

        PortfolioGroupDto.RegisterAssetRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.getStockId()).isEqualTo(101L);
        assertThat(capturedRequest.getName()).isEqualTo("삼성전자");
        assertThat(capturedRequest.getAmount()).isEqualTo(BigDecimal.valueOf(5));
        assertThat(capturedRequest.getStockPrice()).isEqualTo(BigDecimal.valueOf(70000));
        assertThat(capturedRequest.getCurrency()).isEqualTo(Currency.KRW);
    }

    @Test
    @DisplayName("SELL 이벤트 수신 시 unregisterAsset이 올바른 데이터로 호출되어야 한다")
    void handleTradeExecutedEvent_Sell() {
        // given
        Long portfolioId = 20L;
        UUID userId = UUID.randomUUID();

        TradeExecutedEvent event = TradeExecutedEvent.builder()
                .tradeId(2L)
                .userId(userId.toString())
                .type("SELL")
                .amount(BigDecimal.valueOf(3))
                .price(150L)
                .stockId(202L)
                .name("Apple")
                .currency("USD")
                .portfolioId(portfolioId)
                .build();

        // when
        assetEventService.handleTradeExecutedEvent(event);

        // then
        ArgumentCaptor<PortfolioGroupDto.UnregisterAssetRequest> captor =
                ArgumentCaptor.forClass(PortfolioGroupDto.UnregisterAssetRequest.class);

        verify(commandUseCase).unregisterAsset(eq(portfolioId), captor.capture(), eq(userId));

        PortfolioGroupDto.UnregisterAssetRequest capturedRequest = captor.getValue();

        assertThat(capturedRequest.getStockId()).isEqualTo(202L);
        assertThat(capturedRequest.getAmount()).isEqualTo(BigDecimal.valueOf(3));
        assertThat(capturedRequest.getStockPrice()).isEqualTo(BigDecimal.valueOf(150));
        assertThat(capturedRequest.getCurrency()).isEqualTo(Currency.USD);
    }

    @Test
    @DisplayName("알 수 없는 Type(예: HOLD)일 경우 아무 작업도 하지 않아야 한다")
    void handleTradeExecutedEvent_UnknownType() {
        // given
        TradeExecutedEvent event = TradeExecutedEvent.builder()
                .tradeId(3L)
                .userId(UUID.randomUUID().toString())
                .type("HOLD")
                .amount(BigDecimal.TEN)
                .price(10L)
                .stockId(1L)
                .name("Test")
                .currency("KRW")
                .portfolioId(1L)
                .build();

        // when
        assetEventService.handleTradeExecutedEvent(event);

        // then
        verifyNoInteractions(commandUseCase);
    }

    @Test
    @DisplayName("최초 로그인 이벤트 수신 시 기본 포트폴리오를 생성해야 한다")
    void handleSignUpEvent() {
        // given
        UUID userId = UUID.randomUUID();

        SignUpEvent event = SignUpEvent.of(userId.toString());

        // when
        assetEventService.handleSignUpEvent(event);

        // then
        verify(commandUseCase).createDefaultPortfolioGroup(userId);
    }

    @Test
    @DisplayName("자산 이동 이벤트 수신 시 양쪽 포트폴리오의 valuation을 재계산한다")
    void handleAssetTransferredEvent_success() {
        // given
        Long sourcePortfolioId = 1L;
        Long targetPortfolioId = 2L;
        UUID userId = UUID.randomUUID();

        PortfolioGroup sourcePortfolio = PortfolioGroup.builder()
                .id(sourcePortfolioId)
                .name("원본 포트폴리오")
                .userId(userId)
                .assets(new ArrayList<>())
                .build();

        PortfolioGroup targetPortfolio = PortfolioGroup.builder()
                .id(targetPortfolioId)
                .name("대상 포트폴리오")
                .userId(userId)
                .assets(new ArrayList<>())
                .build();

        when(portfolioGroupRepository.findByIdWithAssets(sourcePortfolioId))
                .thenReturn(Optional.of(sourcePortfolio));
        when(portfolioGroupRepository.findByIdWithAssets(targetPortfolioId))
                .thenReturn(Optional.of(targetPortfolio));

        AssetTransferredEvent event = AssetTransferredEvent.builder()
                .sourcePortfolioId(sourcePortfolioId)
                .targetPortfolioId(targetPortfolioId)
                .stockId(100L)
                .merged(false)
                .build();

        // when
        assetEventService.handleAssetTransferredEvent(event);

        // then
        verify(portfolioGroupRepository).findByIdWithAssets(sourcePortfolioId);
        verify(portfolioGroupRepository).findByIdWithAssets(targetPortfolioId);
    }

    @Test
    @DisplayName("자산 이동 이벤트 처리 중 예외 발생 시 로그만 남기고 예외를 전파하지 않는다")
    void handleAssetTransferredEvent_exception_not_propagated() {
        // given
        Long sourcePortfolioId = 1L;
        Long targetPortfolioId = 2L;

        when(portfolioGroupRepository.findByIdWithAssets(sourcePortfolioId))
                .thenThrow(new RuntimeException("DB connection error"));

        AssetTransferredEvent event = AssetTransferredEvent.builder()
                .sourcePortfolioId(sourcePortfolioId)
                .targetPortfolioId(targetPortfolioId)
                .stockId(100L)
                .merged(true)
                .build();

        // when & then
        assertThatCode(() -> assetEventService.handleAssetTransferredEvent(event))
                .doesNotThrowAnyException();
    }
}
