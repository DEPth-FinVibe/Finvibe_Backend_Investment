package depth.finvibe.investment.modules.market.dto;

import depth.finvibe.investment.modules.market.domain.enums.MarketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MarketStatusDto {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {

    private MarketStatus status;

    public static Response from(MarketStatus status) {
      return Response.builder()
          .status(status)
          .build();
    }
  }
}
