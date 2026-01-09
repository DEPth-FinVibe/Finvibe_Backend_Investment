package depth.finvibe.investment.modules.market.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class CurrentPriceTest {

    @Test
    @DisplayName("현재가 생성 시 모든 정보가 설정된다")
    void createCurrentPrice() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // when
        CurrentPrice currentPrice = new CurrentPrice(
                1L,
                now,
                BigDecimal.valueOf(70000),
                BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70500),
                BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(70000000000L)
        );

        // then
        assertThat(currentPrice.stockId()).isEqualTo(1L);
        assertThat(currentPrice.at()).isEqualTo(now);
        assertThat(currentPrice.price()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(currentPrice.open()).isEqualByComparingTo(BigDecimal.valueOf(69000));
        assertThat(currentPrice.high()).isEqualByComparingTo(BigDecimal.valueOf(71000));
        assertThat(currentPrice.low()).isEqualByComparingTo(BigDecimal.valueOf(68000));
        assertThat(currentPrice.close()).isEqualByComparingTo(BigDecimal.valueOf(70500));
        assertThat(currentPrice.prevDayChangePct()).isEqualByComparingTo(BigDecimal.valueOf(1.5));
        assertThat(currentPrice.volume()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(currentPrice.value()).isEqualByComparingTo(BigDecimal.valueOf(70000000000L));
    }

    @Test
    @DisplayName("동일한 값을 가진 현재가는 동등하다")
    void equalsWithSameValues() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CurrentPrice price1 = new CurrentPrice(
                1L, now, BigDecimal.valueOf(70000), BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70500), BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );
        CurrentPrice price2 = new CurrentPrice(
                1L, now, BigDecimal.valueOf(70000), BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70500), BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );

        // when & then
        assertThat(price1).isEqualTo(price2);
        assertThat(price1.hashCode()).isEqualTo(price2.hashCode());
    }

    @Test
    @DisplayName("다른 종목 ID를 가진 현재가는 다르다")
    void notEqualsWithDifferentStockId() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CurrentPrice price1 = new CurrentPrice(
                1L, now, BigDecimal.valueOf(70000), BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70500), BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );
        CurrentPrice price2 = new CurrentPrice(
                2L, now, BigDecimal.valueOf(70000), BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70500), BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );

        // when & then
        assertThat(price1).isNotEqualTo(price2);
    }

    @Test
    @DisplayName("다른 가격을 가진 현재가는 다르다")
    void notEqualsWithDifferentPrice() {
        // given
        LocalDateTime now = LocalDateTime.now();
        CurrentPrice price1 = new CurrentPrice(
                1L, now, BigDecimal.valueOf(70000), BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70500), BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );
        CurrentPrice price2 = new CurrentPrice(
                1L, now, BigDecimal.valueOf(71000), BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70500), BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );

        // when & then
        assertThat(price1).isNotEqualTo(price2);
    }

    @Test
    @DisplayName("null과 비교 시 다르다")
    void notEqualsWithNull() {
        // given
        CurrentPrice price = new CurrentPrice(
                1L, LocalDateTime.now(), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70500),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(70000000000L)
        );

        // when & then
        assertThat(price).isNotEqualTo(null);
    }
}