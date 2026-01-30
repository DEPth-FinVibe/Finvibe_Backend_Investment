package depth.finvibe.investment.modules.market.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import depth.finvibe.investment.modules.market.domain.MarketHours;
import depth.finvibe.investment.modules.market.dto.MarketStatusDto;

import static org.assertj.core.api.Assertions.assertThat;

class MarketStatusServiceTest {

  private final MarketStatusService marketStatusService = new MarketStatusService();

  @Test
  @DisplayName("시장 상태 조회는 현재 시각 기준으로 응답한다")
  void getMarketStatus_returnsCurrentStatus() {
    MarketStatusDto.Response response = marketStatusService.getMarketStatus();

    assertThat(response.getStatus()).isEqualTo(MarketHours.getCurrentStatus());
  }
}
