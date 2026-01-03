package depth.finvibe.investment.modules.trade.domain;

import depth.finvibe.investment.modules.trade.domain.enums.MarketType;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import depth.finvibe.investment.modules.trade.domain.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;
import java.util.UUID;

public class TradeTest {

    @Nested
    @DisplayName("Trade 생성 테스트")
    class CreateTradeTest {

        @Test
        @DisplayName("국내 시장 일반 매수 거래 생성")
        void createDomesticNormalBuyTrade() {
            // given
            Long stockId = 1L;
            Double amount = 10.0;
            Long price = 50000L;
            Long portfolioId = 1L;
            UUID userId = UUID.randomUUID();

            // when
            Trade trade = Trade.builder()
                    .stockId(stockId)
                    .amount(amount)
                    .price(price)
                    .marketType(MarketType.DOMESTIC)
                    .transactionType(TransactionType.BUY)
                    .tradeType(TradeType.NORMAL)
                    .portfolioId(portfolioId)
                    .userId(userId)
                    .build();

            // then
            assertThat(trade.getStockId()).isEqualTo(stockId);
            assertThat(trade.getAmount()).isEqualTo(amount);
            assertThat(trade.getPrice()).isEqualTo(price);
            assertThat(trade.getMarketType()).isEqualTo(MarketType.DOMESTIC);
            assertThat(trade.getTransactionType()).isEqualTo(TransactionType.BUY);
            assertThat(trade.getTradeType()).isEqualTo(TradeType.NORMAL);
            assertThat(trade.getPortfolioId()).isEqualTo(portfolioId);
            assertThat(trade.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("해외 시장 예약 매도 거래 생성")
        void createInternationalReservedSellTrade() {
            // given & when
            Trade trade = Trade.builder()
                    .stockId(1L)
                    .amount(5.0)
                    .price(150L) // USD 가격
                    .marketType(MarketType.INTERNATIONAL)
                    .transactionType(TransactionType.SELL)
                    .tradeType(TradeType.RESERVED)
                    .portfolioId(1L)
                    .userId(UUID.randomUUID())
                    .build();

            // then
            assertThat(trade.getMarketType()).isEqualTo(MarketType.INTERNATIONAL);
            assertThat(trade.getTransactionType()).isEqualTo(TransactionType.SELL);
            assertThat(trade.getTradeType()).isEqualTo(TradeType.RESERVED);
        }

        @Test
        @DisplayName("취소된 거래 생성")
        void createCancelledTrade() {
            // given & when
            Trade trade = Trade.builder()
                    .stockId(1L)
                    .amount(20.0)
                    .price(75000L)
                    .marketType(MarketType.DOMESTIC)
                    .transactionType(TransactionType.BUY)
                    .tradeType(TradeType.CANCELLED)
                    .portfolioId(1L)
                    .userId(UUID.randomUUID())
                    .build();

            // then
            assertThat(trade.getTradeType()).isEqualTo(TradeType.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Enum 값 검증 테스트")
    class EnumValidationTest {

        @ParameterizedTest
        @EnumSource(MarketType.class)
        @DisplayName("모든 MarketType으로 거래 생성 가능")
        void createTradeWithAllMarketTypes(MarketType marketType) {
            // given & when
            Trade trade = Trade.builder()
                    .stockId(1L)
                    .amount(10.0)
                    .price(50000L)
                    .marketType(marketType)
                    .transactionType(TransactionType.BUY)
                    .tradeType(TradeType.NORMAL)
                    .portfolioId(1L)
                    .userId(UUID.randomUUID())
                    .build();

            // then
            assertThat(trade.getMarketType()).isEqualTo(marketType);
        }

        @ParameterizedTest
        @EnumSource(TransactionType.class)
        @DisplayName("모든 TransactionType으로 거래 생성 가능")
        void createTradeWithAllTransactionTypes(TransactionType transactionType) {
            // given & when
            Trade trade = Trade.builder()
                    .stockId(1L)
                    .amount(10.0)
                    .price(50000L)
                    .marketType(MarketType.DOMESTIC)
                    .transactionType(transactionType)
                    .tradeType(TradeType.NORMAL)
                    .portfolioId(1L)
                    .userId(UUID.randomUUID())
                    .build();

            // then
            assertThat(trade.getTransactionType()).isEqualTo(transactionType);
        }

        @ParameterizedTest
        @EnumSource(TradeType.class)
        @DisplayName("모든 TradeType으로 거래 생성 가능")
        void createTradeWithAllTradeTypes(TradeType tradeType) {
            // given & when
            Trade trade = Trade.builder()
                    .stockId(1L)
                    .amount(10.0)
                    .price(50000L)
                    .marketType(MarketType.DOMESTIC)
                    .transactionType(TransactionType.BUY)
                    .tradeType(tradeType)
                    .portfolioId(1L)
                    .userId(UUID.randomUUID())
                    .build();

            // then
            assertThat(trade.getTradeType()).isEqualTo(tradeType);
        }

        @Test
        @DisplayName("MarketType enum 값 검증")
        void validateMarketTypeValues() {
            assertThat(MarketType.values()).containsExactlyInAnyOrder(
                    MarketType.DOMESTIC,
                    MarketType.INTERNATIONAL
            );
        }

        @Test
        @DisplayName("TransactionType enum 값 검증")
        void validateTransactionTypeValues() {
            assertThat(TransactionType.values()).containsExactlyInAnyOrder(
                    TransactionType.BUY,
                    TransactionType.SELL
            );
        }

        @Test
        @DisplayName("TradeType enum 값 검증")
        void validateTradeTypeValues() {
            assertThat(TradeType.values()).containsExactlyInAnyOrder(
                    TradeType.NORMAL,
                    TradeType.RESERVED,
                    TradeType.CANCELLED
            );
        }
    }

    @Nested
    @DisplayName("시나리오별 거래 테스트")
    class ScenarioBasedTradeTest {

        @Test
        @DisplayName("국내 시장 일반 주문 시나리오")
        void domesticNormalTradeScenario() {
            UUID userId = UUID.randomUUID();

            // 국내 매수
            Trade domesticBuy = Trade.builder()
                    .stockId(1L)
                    .amount(100.0)
                    .price(50000L)
                    .marketType(MarketType.DOMESTIC)
                    .transactionType(TransactionType.BUY)
                    .tradeType(TradeType.NORMAL)
                    .portfolioId(1L)
                    .userId(userId)
                    .build();

            // 국내 매도
            Trade domesticSell = Trade.builder()
                    .stockId(1L)
                    .amount(50.0)
                    .price(52000L)
                    .marketType(MarketType.DOMESTIC)
                    .transactionType(TransactionType.SELL)
                    .tradeType(TradeType.NORMAL)
                    .portfolioId(1L)
                    .userId(userId)
                    .build();

            assertThat(domesticBuy.getMarketType()).isEqualTo(MarketType.DOMESTIC);
            assertThat(domesticBuy.getTransactionType()).isEqualTo(TransactionType.BUY);
            assertThat(domesticBuy.getTradeType()).isEqualTo(TradeType.NORMAL);

            assertThat(domesticSell.getMarketType()).isEqualTo(MarketType.DOMESTIC);
            assertThat(domesticSell.getTransactionType()).isEqualTo(TransactionType.SELL);
            assertThat(domesticSell.getTradeType()).isEqualTo(TradeType.NORMAL);
        }

        @Test
        @DisplayName("해외 시장 예약 주문 시나리오")
        void internationalReservedTradeScenario() {
            UUID userId = UUID.randomUUID();

            // 해외 예약 매수
            Trade internationalReservedBuy = Trade.builder()
                    .stockId(100L)
                    .amount(25.0)
                    .price(200L) // USD
                    .marketType(MarketType.INTERNATIONAL)
                    .transactionType(TransactionType.BUY)
                    .tradeType(TradeType.RESERVED)
                    .portfolioId(1L)
                    .userId(userId)
                    .build();

            // 해외 예약 매도
            Trade internationalReservedSell = Trade.builder()
                    .stockId(100L)
                    .amount(10.0)
                    .price(220L)
                    .marketType(MarketType.INTERNATIONAL)
                    .transactionType(TransactionType.SELL)
                    .tradeType(TradeType.RESERVED)
                    .portfolioId(1L)
                    .userId(userId)
                    .build();

            assertThat(internationalReservedBuy.getMarketType()).isEqualTo(MarketType.INTERNATIONAL);
            assertThat(internationalReservedBuy.getTradeType()).isEqualTo(TradeType.RESERVED);

            assertThat(internationalReservedSell.getMarketType()).isEqualTo(MarketType.INTERNATIONAL);
            assertThat(internationalReservedSell.getTradeType()).isEqualTo(TradeType.RESERVED);
        }

        @Test
        @DisplayName("주문 취소 시나리오")
        void tradeCancellationScenario() {
            UUID userId = UUID.randomUUID();

            // 국내 주문 취소
            Trade cancelledDomesticTrade = Trade.builder()
                    .stockId(1L)
                    .amount(75.0)
                    .price(48000L)
                    .marketType(MarketType.DOMESTIC)
                    .transactionType(TransactionType.BUY)
                    .tradeType(TradeType.CANCELLED)
                    .portfolioId(1L)
                    .userId(userId)
                    .build();

            // 해외 주문 취소
            Trade cancelledInternationalTrade = Trade.builder()
                    .stockId(200L)
                    .amount(30.0)
                    .price(180L)
                    .marketType(MarketType.INTERNATIONAL)
                    .transactionType(TransactionType.SELL)
                    .tradeType(TradeType.CANCELLED)
                    .portfolioId(1L)
                    .userId(userId)
                    .build();

            assertThat(cancelledDomesticTrade.getTradeType()).isEqualTo(TradeType.CANCELLED);
            assertThat(cancelledInternationalTrade.getTradeType()).isEqualTo(TradeType.CANCELLED);
        }
    }

    @Nested
    @DisplayName("거래 금액 계산 테스트")
    class TradeAmountTest {

        @Test
        @DisplayName("국내 시장 거래 금액 계산")
        void calculateDomesticTradeAmount() {
            // given
            Double amount = 100.0;
            Long price = 50000L; // KRW

            Trade trade = Trade.builder()
                    .stockId(1L)
                    .amount(amount)
                    .price(price)
                    .marketType(MarketType.DOMESTIC)
                    .transactionType(TransactionType.BUY)
                    .tradeType(TradeType.NORMAL)
                    .portfolioId(1L)
                    .userId(UUID.randomUUID())
                    .build();

            // when
            Double totalAmount = trade.getAmount() * trade.getPrice();

            // then
            assertThat(totalAmount).isEqualTo(5_000_000.0); // 5백만원
        }

        @Test
        @DisplayName("해외 시장 거래 금액 계산")
        void calculateInternationalTradeAmount() {
            // given
            Double amount = 50.0;
            Long price = 150L; // USD

            Trade trade = Trade.builder()
                    .stockId(100L)
                    .amount(amount)
                    .price(price)
                    .marketType(MarketType.INTERNATIONAL)
                    .transactionType(TransactionType.BUY)
                    .tradeType(TradeType.NORMAL)
                    .portfolioId(1L)
                    .userId(UUID.randomUUID())
                    .build();

            // when
            Double totalAmount = trade.getAmount() * trade.getPrice();

            // then
            assertThat(totalAmount).isEqualTo(7500.0); // $7,500
        }

        @Test
        @DisplayName("소수점 거래량 처리")
        void handleDecimalAmount() {
            // given (해외 주식은 소수점 거래 가능)
            Double decimalAmount = 12.75;

            Trade trade = Trade.builder()
                    .stockId(100L)
                    .amount(decimalAmount)
                    .price(200L)
                    .marketType(MarketType.INTERNATIONAL)
                    .transactionType(TransactionType.BUY)
                    .tradeType(TradeType.NORMAL)
                    .portfolioId(1L)
                    .userId(UUID.randomUUID())
                    .build();

            // then
            assertThat(trade.getAmount()).isEqualTo(decimalAmount);
            assertThat(trade.getAmount() * trade.getPrice()).isEqualTo(2550.0);
        }
    }

    @Nested
    @DisplayName("모든 조합 테스트")
    class AllCombinationTest {

        @Test
        @DisplayName("시장타입별 매수/매도 조합 테스트")
        void testAllMarketAndTransactionCombinations() {
            UUID userId = UUID.randomUUID();

            // 국내 + 매수
            Trade domesticBuy = createTrade(MarketType.DOMESTIC, TransactionType.BUY, TradeType.NORMAL, userId);
            // 국내 + 매도
            Trade domesticSell = createTrade(MarketType.DOMESTIC, TransactionType.SELL, TradeType.NORMAL, userId);
            // 해외 + 매수
            Trade internationalBuy = createTrade(MarketType.INTERNATIONAL, TransactionType.BUY, TradeType.NORMAL, userId);
            // 해외 + 매도
            Trade internationalSell = createTrade(MarketType.INTERNATIONAL, TransactionType.SELL, TradeType.NORMAL, userId);

            // 모든 조합이 올바르게 생성되는지 확인
            assertThat(domesticBuy.getMarketType()).isEqualTo(MarketType.DOMESTIC);
            assertThat(domesticBuy.getTransactionType()).isEqualTo(TransactionType.BUY);

            assertThat(domesticSell.getMarketType()).isEqualTo(MarketType.DOMESTIC);
            assertThat(domesticSell.getTransactionType()).isEqualTo(TransactionType.SELL);

            assertThat(internationalBuy.getMarketType()).isEqualTo(MarketType.INTERNATIONAL);
            assertThat(internationalBuy.getTransactionType()).isEqualTo(TransactionType.BUY);

            assertThat(internationalSell.getMarketType()).isEqualTo(MarketType.INTERNATIONAL);
            assertThat(internationalSell.getTransactionType()).isEqualTo(TransactionType.SELL);
        }

        @Test
        @DisplayName("예약 주문 타입별 조합 테스트")
        void testReservedOrderCombinations() {
            UUID userId = UUID.randomUUID();

            // 국내 예약 매수
            Trade domesticReservedBuy = createTrade(MarketType.DOMESTIC, TransactionType.BUY, TradeType.RESERVED, userId);
            // 해외 예약 매도
            Trade internationalReservedSell = createTrade(MarketType.INTERNATIONAL, TransactionType.SELL, TradeType.RESERVED, userId);

            assertThat(domesticReservedBuy.getTradeType()).isEqualTo(TradeType.RESERVED);
            assertThat(internationalReservedSell.getTradeType()).isEqualTo(TradeType.RESERVED);
        }

        private Trade createTrade(MarketType marketType, TransactionType transactionType, TradeType tradeType, UUID userId) {
            return Trade.builder()
                    .stockId(1L)
                    .amount(10.0)
                    .price(marketType == MarketType.DOMESTIC ? 50000L : 150L)
                    .marketType(marketType)
                    .transactionType(transactionType)
                    .tradeType(tradeType)
                    .portfolioId(1L)
                    .userId(userId)
                    .build();
        }
    }
}