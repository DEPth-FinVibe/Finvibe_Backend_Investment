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
        PriceCandle candle = new PriceCandle(
                1L,
                Timeframe.DAY,
                now,
                BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70000),
                1.5f,
                1000000L,
                70000000000L
        );

        // then
        assertThat(candle.stockId()).isEqualTo(1L);
        assertThat(candle.timeframe()).isEqualTo(Timeframe.DAY);
        assertThat(candle.at()).isEqualTo(now);
        assertThat(candle.open()).isEqualByComparingTo(BigDecimal.valueOf(69000));
        assertThat(candle.high()).isEqualByComparingTo(BigDecimal.valueOf(71000));
        assertThat(candle.low()).isEqualByComparingTo(BigDecimal.valueOf(68000));
        assertThat(candle.close()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(candle.prevDayChangePct()).isEqualTo(1.5f);
        assertThat(candle.volume()).isEqualTo(1000000L);
        assertThat(candle.value()).isEqualTo(70000000000L);
    }

    @Test
    @DisplayName("동일한 값을 가진 캔들은 동등하다")
    void equalsWithSameValues() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PriceCandle candle1 = new PriceCandle(
                1L, Timeframe.MINUTE, now, BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70000), 1.5f, 1000000L, 70000000000L
        );
        PriceCandle candle2 = new PriceCandle(
                1L, Timeframe.MINUTE, now, BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70000), 1.5f, 1000000L, 70000000000L
        );

        // when & then
        assertThat(candle1).isEqualTo(candle2);
    }

    @Test
    @DisplayName("다른 종목 ID를 가진 캔들은 다르다")
    void notEqualsWithDifferentStockId() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PriceCandle candle1 = new PriceCandle(
                1L, Timeframe.HOUR, now, BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70000), 1.5f, 1000000L, 70000000000L
        );
        PriceCandle candle2 = new PriceCandle(
                2L, Timeframe.HOUR, now, BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70000), 1.5f, 1000000L, 70000000000L
        );

        // when & then
        assertThat(candle1).isNotEqualTo(candle2);
    }

    @Test
    @DisplayName("다른 타임프레임을 가진 캔들은 다르다")
    void notEqualsWithDifferentTimeframe() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PriceCandle candle1 = new PriceCandle(
                1L, Timeframe.HOUR, now, BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70000), 1.5f, 1000000L, 70000000000L
        );
        PriceCandle candle2 = new PriceCandle(
                1L, Timeframe.DAY, now, BigDecimal.valueOf(69000),
                BigDecimal.valueOf(71000), BigDecimal.valueOf(68000),
                BigDecimal.valueOf(70000), 1.5f, 1000000L, 70000000000L
        );

        // when & then
        assertThat(candle1).isNotEqualTo(candle2);
    }

    @Test
    @DisplayName("null과 비교 시 다르다")
    void notEqualsWithNull() {
        // given
        PriceCandle candle = new PriceCandle(
                1L, Timeframe.DAY, LocalDateTime.now(),
                BigDecimal.valueOf(69000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(68000), BigDecimal.valueOf(70000),
                1.5f, 1000000L, 70000000000L
        );

        // when & then
        assertThat(candle).isNotEqualTo(null);
    }
}