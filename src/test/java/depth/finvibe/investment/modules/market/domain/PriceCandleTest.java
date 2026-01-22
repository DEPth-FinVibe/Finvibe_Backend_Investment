package depth.finvibe.investment.modules.market.domain;

import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class PriceCandleTest {

    @Test
    @DisplayName("가격 캔들 생성 시 모든 정보가 설정된다")
    void createPriceCandle() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // when
        PriceCandle candle = PriceCandle.create(
                1L,
                Timeframe.DAY,
                now,
                BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70000),
                BigDecimal.valueOf(1.5),
                BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(70000000000L)
        );

        // then
        assertThat(candle).isNotNull();
    }

    @Test
    @DisplayName("동일한 stockId, timeframe, at을 가진 캔들은 동등하다")
    void equalsWithSameIdentity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PriceCandle candle1 = PriceCandle.create(
                1L, Timeframe.MINUTE, now,
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );
        PriceCandle candle2 = PriceCandle.create(
                1L, Timeframe.MINUTE, now,
                BigDecimal.valueOf(80000), BigDecimal.valueOf(85000),
                BigDecimal.valueOf(78000), BigDecimal.valueOf(82000),
                BigDecimal.valueOf(2.0), BigDecimal.valueOf(2000000), BigDecimal.valueOf(160000000000L)
        );

        // when & then
        assertThat(candle1).isEqualTo(candle2);
        assertThat(candle1.hashCode()).isEqualTo(candle2.hashCode());
    }

    @Test
    @DisplayName("다른 종목 ID를 가진 캔들은 다르다")
    void notEqualsWithDifferentStockId() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PriceCandle candle1 = PriceCandle.create(
                1L, Timeframe.MINUTE, now,
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );
        PriceCandle candle2 = PriceCandle.create(
                2L, Timeframe.MINUTE, now,
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );

        // when & then
        assertThat(candle1).isNotEqualTo(candle2);
    }

    @Test
    @DisplayName("다른 타임프레임을 가진 캔들은 다르다")
    void notEqualsWithDifferentTimeframe() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PriceCandle candle1 = PriceCandle.create(
                1L, Timeframe.MINUTE, now,
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );
        PriceCandle candle2 = PriceCandle.create(
                1L, Timeframe.DAY, now,
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );

        // when & then
        assertThat(candle1).isNotEqualTo(candle2);
    }

    @Test
    @DisplayName("다른 시간을 가진 캔들은 다르다")
    void notEqualsWithDifferentAt() {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime later = now.plusMinutes(1);
        PriceCandle candle1 = PriceCandle.create(
                1L, Timeframe.MINUTE, now,
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );
        PriceCandle candle2 = PriceCandle.create(
                1L, Timeframe.MINUTE, later,
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );

        // when & then
        assertThat(candle1).isNotEqualTo(candle2);
    }

    @Test
    @DisplayName("null과 비교 시 다르다")
    void notEqualsWithNull() {
        // given
        PriceCandle candle = PriceCandle.create(
                1L, Timeframe.DAY, LocalDateTime.now(),
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70000),
                BigDecimal.valueOf(1.5), BigDecimal.valueOf(1000000), BigDecimal.valueOf(70000000000L)
        );

        // when & then
        assertThat(candle).isNotEqualTo(null);
    }
}
