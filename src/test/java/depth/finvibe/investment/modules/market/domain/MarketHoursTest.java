package depth.finvibe.investment.modules.market.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import depth.finvibe.investment.modules.market.domain.enums.MarketStatus;

import static org.assertj.core.api.Assertions.assertThat;

class MarketHoursTest {

  @Test
  @DisplayName("평일 장 운영 시간에는 OPEN이다")
  void getStatusAt_returnsOpenDuringMarketHours() {
    ZonedDateTime dateTime = dateTime(2024, 1, 2, 10, 0);

    MarketStatus status = MarketHours.getStatusAt(dateTime);

    assertThat(status).isEqualTo(MarketStatus.OPEN);
  }

  @Test
  @DisplayName("평일 장 시작 전에는 CLOSED이다")
  void getStatusAt_returnsClosedBeforeMarketOpen() {
    ZonedDateTime dateTime = dateTime(2024, 1, 2, 8, 59);

    MarketStatus status = MarketHours.getStatusAt(dateTime);

    assertThat(status).isEqualTo(MarketStatus.CLOSED);
  }

  @Test
  @DisplayName("평일 장 마감 이후에는 CLOSED이다")
  void getStatusAt_returnsClosedAfterMarketClose() {
    ZonedDateTime dateTime = dateTime(2024, 1, 2, 15, 30);

    MarketStatus status = MarketHours.getStatusAt(dateTime);

    assertThat(status).isEqualTo(MarketStatus.CLOSED);
  }

  @Test
  @DisplayName("주말에는 CLOSED이다")
  void getStatusAt_returnsClosedOnWeekend() {
    ZonedDateTime dateTime = dateTime(2024, 1, 6, 10, 0);

    MarketStatus status = MarketHours.getStatusAt(dateTime);

    assertThat(status).isEqualTo(MarketStatus.CLOSED);
  }

  private ZonedDateTime dateTime(int year, int month, int day, int hour, int minute) {
    return ZonedDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute), ZoneId.of("Asia/Seoul"));
  }
}
