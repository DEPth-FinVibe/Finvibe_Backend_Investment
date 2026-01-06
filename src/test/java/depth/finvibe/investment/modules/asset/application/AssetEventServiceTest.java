package depth.finvibe.investment.modules.asset.application;

import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.dto.FirstLoginedEvent;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.modules.asset.dto.TradeExecutedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AssetEventServiceTest {

    @Mock
    private AssetCommandUseCase commandUseCase;

    @InjectMocks
    private AssetEventService assetEventService;

    @Test
    @DisplayName("BUY 이벤트 수신 시 registerAsset이 올바른 데이터로 호출되어야 한다")
    void handleTradeExecutedEvent_Buy() {
        // given
        Long portfolioId = 10L;
        UUID userId = UUID.randomUUID();

        TradeExecutedEvent event = new TradeExecutedEvent(
                101L,                           // stockId
                BigDecimal.valueOf(5),          // amount
                BigDecimal.valueOf(70000),      // stockPrice
                "삼성전자",                      // name
                Currency.KRW,                   // currency
                portfolioId,                    // portfolioId
                userId.toString(),              // userId
                "BUY"                           // type
        );

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

        TradeExecutedEvent event = new TradeExecutedEvent(
                202L,                           // stockId
                BigDecimal.valueOf(3),          // amount
                BigDecimal.valueOf(150),        // stockPrice
                "Apple",                        // name (매도 시 보통 무시되지만 이벤트엔 포함됨)
                Currency.USD,                   // currency
                portfolioId,                    // portfolioId
                userId.toString(),              // userId
                "SELL"                          // type
        );

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
        TradeExecutedEvent event = new TradeExecutedEvent(
                1L, BigDecimal.TEN, BigDecimal.TEN, "Test", Currency.KRW,
                1L, UUID.randomUUID().toString(),
                "HOLD" // 정의되지 않은 타입
        );

        // when
        assetEventService.handleTradeExecutedEvent(event);

        // then
        verifyNoInteractions(commandUseCase);
    }

    @Test
    @DisplayName("최초 로그인 이벤트 수신 시 기본 포트폴리오를 생성해야 한다")
    void handleFirstLoginedEvent() {
        // given
        UUID userId = UUID.randomUUID();

        FirstLoginedEvent event = new FirstLoginedEvent(userId.toString());

        // when
        assetEventService.handleFirstLoginedEvent(event);

        // then
        verify(commandUseCase).createDefaultPortfolioGroup(userId);
    }
}