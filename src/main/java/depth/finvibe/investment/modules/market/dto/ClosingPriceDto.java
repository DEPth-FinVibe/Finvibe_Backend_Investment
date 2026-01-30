package depth.finvibe.investment.modules.market.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.Stock;

public class ClosingPriceDto {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {

    private Long stockId;
    private String stockName;
    private LocalDateTime at;
    private BigDecimal close;
    private BigDecimal prevDayChangePct;
    private BigDecimal volume;
    private BigDecimal value;

    public static Response from(PriceCandle candle, Stock stock) {
      return Response.builder()
          .stockId(candle.getStockId())
          .stockName(stock.getName())
          .at(candle.getAt())
          .close(candle.getClose())
          .prevDayChangePct(candle.getPrevDayChangePct())
          .volume(candle.getVolume())
          .value(candle.getValue())
          .build();
    }
  }
}
