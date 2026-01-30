package depth.finvibe.investment.modules.market.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
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
  @Schema(name = "ClosingPriceResponse", description = "종가 응답")
  public static class Response {

    @Schema(description = "종목 ID", example = "1")
    private Long stockId;
    @Schema(description = "종목명", example = "삼성전자")
    private String stockName;
    @Schema(description = "기준 시각", example = "2024-01-02T15:30:00")
    private LocalDateTime at;
    @Schema(description = "종가", example = "70000")
    private BigDecimal close;
    @Schema(description = "전일 대비 등락률", example = "0.5")
    private BigDecimal prevDayChangePct;
    @Schema(description = "거래량", example = "12000000")
    private BigDecimal volume;
    @Schema(description = "거래대금", example = "840000000000")
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
