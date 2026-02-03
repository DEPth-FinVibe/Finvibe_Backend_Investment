package depth.finvibe.investment.modules.trade.application;

import depth.finvibe.investment.boot.security.model.Requester;
import depth.finvibe.investment.boot.security.model.UserRole;
import depth.finvibe.investment.modules.trade.application.port.out.AssetClient;
import depth.finvibe.investment.modules.trade.application.port.out.MarketClient;
import depth.finvibe.investment.modules.trade.application.port.out.TradeEventProducer;
import depth.finvibe.investment.modules.trade.application.port.out.TradeRepository;
import depth.finvibe.investment.modules.trade.application.port.out.WalletClient;
import depth.finvibe.investment.modules.trade.domain.Trade;
import depth.finvibe.investment.modules.trade.domain.enums.MarketType;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import depth.finvibe.investment.modules.trade.domain.enums.TransactionType;
import depth.finvibe.investment.modules.trade.domain.error.TradeErrorCode;
import depth.finvibe.investment.modules.trade.dto.TradeDto;
import depth.finvibe.investment.shared.application.port.out.GamificationEventProducer;
import depth.finvibe.investment.shared.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeService 테스트")
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeEventProducer tradeEventProducer;

    @Mock
    private GamificationEventProducer gamificationEventProducer;

    @Mock
    private AssetClient assetClient;

    @Mock
    private MarketClient marketClient;

    @Mock
    private WalletClient walletClient;

    @InjectMocks
    private TradeService tradeService;

    private TradeDto.TransactionRequest normalBuyRequest;
    private TradeDto.TransactionRequest reservedBuyRequest;
    private Trade normalTrade;
    private Trade reservedTrade;
    private UUID userId;
    private Requester requester;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        requester = Requester.builder()
                .uuid(userId)
                .role(UserRole.USER)
                .build();

        normalBuyRequest = TradeDto.TransactionRequest.builder()
                .marketType(MarketType.DOMESTIC)
                .stockId(5930L)
                .amount(10.0)
                .price(70000L)
                .portfolioId(1L)
                .userId(userId)
                .transactionType(TransactionType.BUY)
                .tradeType(TradeType.NORMAL)
                .build();

        reservedBuyRequest = TradeDto.TransactionRequest.builder()
                .marketType(MarketType.DOMESTIC)
                .stockId(5930L)
                .amount(10.0)
                .price(70000L)
                .portfolioId(1L)
                .userId(userId)
                .transactionType(TransactionType.BUY)
                .tradeType(TradeType.RESERVED)
                .build();

        normalTrade = Trade.create(
                MarketType.DOMESTIC,
                5930L,
                10.0,
                70000L,
                1L,
                userId,
                TransactionType.BUY,
                TradeType.NORMAL
        );

        reservedTrade = Trade.create(
                MarketType.DOMESTIC,
                5930L,
                10.0,
                70000L,
                1L,
                userId,
                TransactionType.BUY,
                TradeType.RESERVED
        );
    }

    @Test
    @DisplayName("거래 조회 성공")
    void getTrade_Success() {
        // given
        given(tradeRepository.findById(1L)).willReturn(Optional.of(normalTrade));

        // when
        TradeDto.TradeResponse response = tradeService.findTrade(1L);

        // then
        assertThat(response.getStockId()).isEqualTo(5930L);
        assertThat(response.getAmount()).isEqualTo(10.0);
        verify(tradeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("거래 조회 실패 - 거래를 찾을 수 없음")
    void getTrade_NotFound() {
        // given
        given(tradeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tradeService.findTrade(1L))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.TRADE_NOT_FOUND);
    }

    @Test
    @DisplayName("일반 거래 생성 성공")
    void createNormalTrade_Success() {
        // given
        stubTradeContexts();
        given(tradeRepository.save(any(Trade.class))).willReturn(normalTrade);

        // when
        TradeDto.TradeResponse response = tradeService.createTrade(normalBuyRequest, requester);

        // then
        assertThat(response.getStockId()).isEqualTo(5930L);
        verify(tradeRepository, times(1)).save(any(Trade.class));
        verify(tradeEventProducer, times(1)).publishNormalTradeExecutedEvent(any(Trade.class));
    }


    @Test
    @DisplayName("예약 거래 생성 성공")
    void createReservedTrade_Success() {
        // given
        stubTradeContexts();
        given(tradeRepository.save(any(Trade.class))).willReturn(reservedTrade);

        // when
        TradeDto.TradeResponse response = tradeService.createTrade(reservedBuyRequest, requester);

        // then
        assertThat(response.getStockId()).isEqualTo(5930L);
        assertThat(response.getTradeType()).isEqualTo(TradeType.RESERVED);
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }

    @Test
    @DisplayName("매도 거래 생성 성공")
    void createSellTrade_Success() {
        // given
        stubTradeContexts();
        TradeDto.TransactionRequest sellRequest = TradeDto.TransactionRequest.builder()
                .marketType(MarketType.DOMESTIC)
                .stockId(5930L)
                .amount(5.0)
                .price(75000L)
                .portfolioId(1L)
                .userId(userId)
                .transactionType(TransactionType.SELL)
                .tradeType(TradeType.NORMAL)
                .build();

        Trade sellTrade = Trade.create(
                MarketType.DOMESTIC,
                5930L,
                5.0,
                75000L,
                1L,
                userId,
                TransactionType.SELL,
                TradeType.NORMAL
        );

        given(tradeRepository.save(any(Trade.class))).willReturn(sellTrade);

        // when
        TradeDto.TradeResponse response = tradeService.createTrade(sellRequest, requester);

        // then
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.SELL);
        assertThat(response.getAmount()).isEqualTo(5.0);
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }

    @Test
    @DisplayName("잘못된 거래 타입으로 생성 실패")
    void createTrade_InvalidTradeType() {
        // given
        stubTradeContexts();
        TradeDto.TransactionRequest invalidRequest = TradeDto.TransactionRequest.builder()
                .marketType(MarketType.DOMESTIC)
                .stockId(5930L)
                .amount(10.0)
                .price(70000L)
                .portfolioId(1L)
                .userId(userId)
                .transactionType(TransactionType.BUY)
                .tradeType(null)
                .build();

        // when & then
        assertThatThrownBy(() -> tradeService.createTrade(invalidRequest, requester))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_TRADE_TYPE);
    }

    @Test
    @DisplayName("예약 거래 취소 성공")
    void cancelTrade_Success() {
        // given
        Trade spyReservedTrade = spy(reservedTrade);
        given(tradeRepository.findById(1L)).willReturn(Optional.of(spyReservedTrade));
        given(tradeRepository.save(any(Trade.class))).willReturn(spyReservedTrade);

        // when
        TradeDto.TradeResponse response = tradeService.cancelTrade(1L, requester);

        // then
        verify(spyReservedTrade, times(1)).cancel();
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }

    @Test
    @DisplayName("거래 취소 실패 - 거래를 찾을 수 없음")
    void cancelTrade_NotFound() {
        // given
        given(tradeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tradeService.cancelTrade(1L, requester))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.TRADE_NOT_FOUND);
    }

    @Test
    @DisplayName("거래 취소 실패 - 일반 거래는 취소 불가")
    void cancelTrade_NormalTradeNotCancellable() {
        // given
        given(tradeRepository.findById(1L)).willReturn(Optional.of(normalTrade));

        // when & then
        assertThatThrownBy(() -> tradeService.cancelTrade(1L, requester))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.RESERVED_TRADE_ONLY_CANCELLABLE);
    }

    private void stubTradeContexts() {
        given(marketClient.isMarketOpen()).willReturn(true);
        given(assetClient.isExistPortfolio(eq(1L), eq(userId))).willReturn(true);
        given(walletClient.getWalletBalance(eq(userId))).willReturn(1_000_000L);
    }

    @Test
    @DisplayName("예약 주문 체결 성공")
    void executeReservedTrade_Success() {
        // given
        Trade spyReservedTrade = spy(reservedTrade);
        given(tradeRepository.findById(1L)).willReturn(Optional.of(spyReservedTrade));
        given(tradeRepository.save(any(Trade.class))).willReturn(spyReservedTrade);

        // when
        TradeDto.TradeResponse response = tradeService.executeReservedTrade(1L);

        // then
        verify(spyReservedTrade, times(1)).execute();
        verify(tradeRepository, times(1)).save(any(Trade.class));
        verify(tradeEventProducer, times(1)).publishReservedTradeExecutedEvent(any(Trade.class));
    }

    @Test
    @DisplayName("예약 주문 체결 실패 - 거래를 찾을 수 없음")
    void executeReservedTrade_NotFound() {
        // given
        given(tradeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tradeService.executeReservedTrade(1L))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.TRADE_NOT_FOUND);
    }

    @Test
    @DisplayName("예약 주문 체결 실패 - 일반 거래는 체결 불가")
    void executeReservedTrade_InvalidTradeType() {
        // given
        given(tradeRepository.findById(1L)).willReturn(Optional.of(normalTrade));

        // when & then
        assertThatThrownBy(() -> tradeService.executeReservedTrade(1L))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", TradeErrorCode.INVALID_TRADE_TYPE);
    }
}
