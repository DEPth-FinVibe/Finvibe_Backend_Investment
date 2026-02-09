package depth.finvibe.investment.modules.market.infra.client;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.infra.client.dto.KisDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RealMarketClientImpl 날짜/시간 파싱 예외 처리 테스트")
class RealMarketClientImplTest {

  @Mock
  private KisApiClient kisApiClient;

  @Mock
  private StockRepository stockRepository;

  @Test
  @DisplayName("분봉 조회 시 잘못된 날짜/시간 레코드는 건너뛴다")
  void fetchIntradayCandles_skipInvalidDateTimeRecord() {
    // Given
    RealMarketClientImpl client = new RealMarketClientImpl(kisApiClient, List.of(), stockRepository);
    Stock stock = Stock.builder()
            .id(1L)
            .name("하이닉스")
            .symbol("000660")
            .build();
    when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

    KisDto.TimeDailyChartPriceResponse response = KisDto.TimeDailyChartPriceResponse.builder()
            .output1(KisDto.TimeDailyChartPriceOutput1.builder().stck_prdy_clpr("2500").build())
            .output2(List.of(
                    KisDto.TimeDailyChartPriceOutput2.builder()
                            .stck_bsop_date("99999999")
                            .stck_cntg_hour("999999")
                            .stck_prpr("2500")
                            .stck_oprc("2500")
                            .stck_hgpr("2500")
                            .stck_lwpr("2500")
                            .cntg_vol("100")
                            .acml_tr_pbmn("10000")
                            .build(),
                    KisDto.TimeDailyChartPriceOutput2.builder()
                            .stck_bsop_date("20260206")
                            .stck_cntg_hour("093000")
                            .stck_prpr("2510")
                            .stck_oprc("2505")
                            .stck_hgpr("2515")
                            .stck_lwpr("2500")
                            .cntg_vol("200")
                            .acml_tr_pbmn("20000")
                            .build()
            ))
            .build();

    when(kisApiClient.fetchTimeDailyChartPrice(eq("J"), eq("000660"), anyString(), anyString(), eq("Y"), isNull()))
            .thenReturn(response);

    // When
    List<PriceCandleDto.Response> result = client.fetchPriceCandles(
            1L,
            LocalDateTime.of(2026, 2, 6, 9, 0),
            LocalDateTime.of(2026, 2, 6, 9, 30),
            Timeframe.MINUTE
    );

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAt()).isEqualTo(LocalDateTime.of(2026, 2, 6, 9, 30));
  }

  @Test
  @DisplayName("일봉 조회 시 잘못된 날짜 레코드는 건너뛴다")
  void fetchDailyCandles_skipInvalidDateRecord() {
    // Given
    RealMarketClientImpl client = new RealMarketClientImpl(kisApiClient, List.of(), stockRepository);
    Stock stock = Stock.builder()
            .id(1L)
            .name("하이닉스")
            .symbol("000660")
            .build();
    when(stockRepository.findById(1L)).thenReturn(Optional.of(stock));

    KisDto.DailyItemChartPriceResponse response = KisDto.DailyItemChartPriceResponse.builder()
            .output2(List.of(
                    KisDto.DailyItemChartPriceOutput2.builder()
                            .stck_bsop_date("99999999")
                            .stck_clpr("2500")
                            .stck_oprc("2490")
                            .stck_hgpr("2510")
                            .stck_lwpr("2480")
                            .acml_vol("1000")
                            .acml_tr_pbmn("100000")
                            .prdy_vrss("10")
                            .prdy_vrss_sign("2")
                            .build(),
                    KisDto.DailyItemChartPriceOutput2.builder()
                            .stck_bsop_date("20260206")
                            .stck_clpr("2510")
                            .stck_oprc("2500")
                            .stck_hgpr("2520")
                            .stck_lwpr("2490")
                            .acml_vol("2000")
                            .acml_tr_pbmn("200000")
                            .prdy_vrss("10")
                            .prdy_vrss_sign("2")
                            .build()
            ))
            .build();

    when(kisApiClient.fetchDailyItemChartPrice(eq("J"), eq("000660"), anyString(), anyString(), eq("D"), eq("1")))
            .thenReturn(response);

    // When
    List<PriceCandleDto.Response> result = client.fetchPriceCandles(
            1L,
            LocalDateTime.of(2026, 2, 1, 0, 0),
            LocalDateTime.of(2026, 2, 10, 23, 59),
            Timeframe.DAY
    );

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAt()).isEqualTo(LocalDateTime.of(2026, 2, 6, 0, 0));
  }
}
