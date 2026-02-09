package depth.finvibe.investment.modules.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import depth.finvibe.investment.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.enums.MarketIndexType;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.infra.client.KisApiClient;
import depth.finvibe.investment.modules.market.infra.client.dto.KisDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexMinuteCandleCacheService 지수 분봉 초기화 테스트")
class IndexMinuteCandleCacheServiceInitTest {

  @Mock
  private KisApiClient kisApiClient;

  @Mock
  private StockRepository stockRepository;

  @Mock
  private PriceCandleRepository priceCandleRepository;

  @InjectMocks
  private IndexMinuteCandleCacheService service;

  @Test
  @DisplayName("데이터가 없을 때 KIS API 호출 후 저장한다")
  void initializeIndexMinuteCandles_success() {
    // Given
    MarketIndexType indexType = MarketIndexType.KOSPI;
    Stock indexStock = Stock.builder()
            .id(1L)
            .name("코스피")
            .symbol("INDEX_KOSPI")
            .build();

    when(stockRepository.findBySymbol("INDEX_KOSPI")).thenReturn(Optional.of(indexStock));
    when(priceCandleRepository.existsByStockIdAndTimeframe(1L, Timeframe.MINUTE)).thenReturn(false);

    List<KisDto.IndexTimePriceOutput> outputs = List.of(
            createIndexOutput("093000", "2500.50", "100", "1000000"),
            createIndexOutput("093100", "2501.00", "110", "1100000")
    );
    when(kisApiClient.fetchIndexTimePrice(eq(KisApiClient.IndexCode.KOSPI), eq("60")))
            .thenReturn(outputs);

    when(priceCandleRepository.findByStockIdAndTimeframeAndAtIn(eq(1L), eq(Timeframe.MINUTE), anyList()))
            .thenReturn(Collections.emptyList());

    // When
    service.initializeIndexMinuteCandlesIfEmpty(indexType);

    // Then
    ArgumentCaptor<List<PriceCandle>> captor = ArgumentCaptor.forClass(List.class);
    verify(priceCandleRepository).saveAll(captor.capture());
    List<PriceCandle> savedCandles = captor.getValue();

    assertThat(savedCandles).hasSize(2);
    assertThat(savedCandles.get(0).getStockId()).isEqualTo(1L);
    assertThat(savedCandles.get(0).getTimeframe()).isEqualTo(Timeframe.MINUTE);
    assertThat(savedCandles.get(0).getClose()).isEqualByComparingTo(new BigDecimal("2500.50"));
  }

  @Test
  @DisplayName("데이터가 이미 존재하면 KIS API 호출하지 않는다")
  void initializeIndexMinuteCandles_skipIfExists() {
    // Given
    MarketIndexType indexType = MarketIndexType.KOSDAQ;
    Stock indexStock = Stock.builder()
            .id(2L)
            .name("코스닥")
            .symbol("INDEX_KOSDAQ")
            .build();

    when(stockRepository.findBySymbol("INDEX_KOSDAQ")).thenReturn(Optional.of(indexStock));
    when(priceCandleRepository.existsByStockIdAndTimeframe(2L, Timeframe.MINUTE)).thenReturn(true);

    // When
    service.initializeIndexMinuteCandlesIfEmpty(indexType);

    // Then
    verify(kisApiClient, never()).fetchIndexTimePrice(any(), any());
    verify(priceCandleRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("Stock이 없으면 생성 후 저장한다")
  void initializeIndexMinuteCandles_createStockIfMissing() {
    // Given
    MarketIndexType indexType = MarketIndexType.KOSPI;

    // findBySymbol이 empty를 반환하면 getOrCreateIndexStock이 새 Stock을 생성하고 save 호출
    // initializeIndexMinuteCandlesIfEmpty와 cacheIndexMinuteCandles에서 각각 getOrCreateIndexStock을 호출
    // 첫 번째 호출: initializeIndexMinuteCandlesIfEmpty에서 -> empty 반환 -> save 호출
    // 두 번째 호출: cacheIndexMinuteCandles에서 -> empty 반환 -> save 호출
    when(stockRepository.findBySymbol("INDEX_KOSPI")).thenReturn(Optional.empty());

    // existsByStockIdAndTimeframe은 생성된 Stock의 ID로 호출됨 (null일 수 있음)
    when(priceCandleRepository.existsByStockIdAndTimeframe(any(), eq(Timeframe.MINUTE))).thenReturn(false);

    List<KisDto.IndexTimePriceOutput> outputs = List.of(
            createIndexOutput("100000", "2600.00", "200", "2000000")
    );
    when(kisApiClient.fetchIndexTimePrice(eq(KisApiClient.IndexCode.KOSPI), eq("60")))
            .thenReturn(outputs);

    when(priceCandleRepository.findByStockIdAndTimeframeAndAtIn(any(), eq(Timeframe.MINUTE), anyList()))
            .thenReturn(Collections.emptyList());

    // When
    service.initializeIndexMinuteCandlesIfEmpty(indexType);

    // Then
    // getOrCreateIndexStock이 두 번 호출되므로 save도 두 번 호출됨
    verify(stockRepository, times(2)).save(any(Stock.class));
    verify(priceCandleRepository, times(1)).saveAll(anyList());
  }

  @Test
  @DisplayName("KIS API 빈 응답 시 저장하지 않는다")
  void initializeIndexMinuteCandles_emptyResponse() {
    // Given
    MarketIndexType indexType = MarketIndexType.KOSPI;
    Stock indexStock = Stock.builder()
            .id(1L)
            .name("코스피")
            .symbol("INDEX_KOSPI")
            .build();

    when(stockRepository.findBySymbol("INDEX_KOSPI")).thenReturn(Optional.of(indexStock));
    when(priceCandleRepository.existsByStockIdAndTimeframe(1L, Timeframe.MINUTE)).thenReturn(false);
    when(kisApiClient.fetchIndexTimePrice(eq(KisApiClient.IndexCode.KOSPI), eq("60")))
            .thenReturn(Collections.emptyList());

    // When
    service.initializeIndexMinuteCandlesIfEmpty(indexType);

    // Then
    verify(priceCandleRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("중복 분봉을 필터링하여 저장한다")
  void initializeIndexMinuteCandles_filterDuplicates() {
    // Given
    MarketIndexType indexType = MarketIndexType.KOSPI;
    Stock indexStock = Stock.builder()
            .id(1L)
            .name("코스피")
            .symbol("INDEX_KOSPI")
            .build();

    when(stockRepository.findBySymbol("INDEX_KOSPI")).thenReturn(Optional.of(indexStock));
    when(priceCandleRepository.existsByStockIdAndTimeframe(1L, Timeframe.MINUTE)).thenReturn(false);

    List<KisDto.IndexTimePriceOutput> outputs = List.of(
            createIndexOutput("093000", "2500.50", "100", "1000000"),
            createIndexOutput("093100", "2501.00", "110", "1100000"),
            createIndexOutput("093200", "2502.00", "120", "1200000")
    );
    when(kisApiClient.fetchIndexTimePrice(eq(KisApiClient.IndexCode.KOSPI), eq("60")))
            .thenReturn(outputs);

    // 093100은 이미 존재
    LocalDateTime existingTime = LocalDateTime.now().withHour(9).withMinute(31).withSecond(0).withNano(0);
    PriceCandle existingCandle = PriceCandle.builder()
            .stockId(1L)
            .timeframe(Timeframe.MINUTE)
            .at(existingTime)
            .build();

    when(priceCandleRepository.findByStockIdAndTimeframeAndAtIn(eq(1L), eq(Timeframe.MINUTE), anyList()))
            .thenReturn(List.of(existingCandle));

    // When
    service.initializeIndexMinuteCandlesIfEmpty(indexType);

    // Then
    ArgumentCaptor<List<PriceCandle>> captor = ArgumentCaptor.forClass(List.class);
    verify(priceCandleRepository).saveAll(captor.capture());
    List<PriceCandle> savedCandles = captor.getValue();

    // 3개 중 1개는 기존 데이터이므로 2개만 저장되어야 함
    assertThat(savedCandles).hasSize(2);
  }

  @Test
  @DisplayName("잘못된 시간 데이터는 건너뛰고 정상 분봉만 저장한다")
  void initializeIndexMinuteCandles_skipInvalidTime() {
    // Given
    MarketIndexType indexType = MarketIndexType.KOSPI;
    Stock indexStock = Stock.builder()
            .id(1L)
            .name("코스피")
            .symbol("INDEX_KOSPI")
            .build();

    when(stockRepository.findBySymbol("INDEX_KOSPI")).thenReturn(Optional.of(indexStock));
    when(priceCandleRepository.existsByStockIdAndTimeframe(1L, Timeframe.MINUTE)).thenReturn(false);

    List<KisDto.IndexTimePriceOutput> outputs = List.of(
            createIndexOutput("999999", "2500.50", "100", "1000000"),
            createIndexOutput("888888", "2501.00", "110", "1100000"),
            createIndexOutput("153000", "2502.00", "120", "1200000")
    );
    when(kisApiClient.fetchIndexTimePrice(eq(KisApiClient.IndexCode.KOSPI), eq("60")))
            .thenReturn(outputs);

    when(priceCandleRepository.findByStockIdAndTimeframeAndAtIn(eq(1L), eq(Timeframe.MINUTE), anyList()))
            .thenReturn(Collections.emptyList());

    // When
    service.initializeIndexMinuteCandlesIfEmpty(indexType);

    // Then
    ArgumentCaptor<List<PriceCandle>> captor = ArgumentCaptor.forClass(List.class);
    verify(priceCandleRepository).saveAll(captor.capture());
    List<PriceCandle> savedCandles = captor.getValue();

    assertThat(savedCandles).hasSize(1);
    assertThat(savedCandles.get(0).getAt().toLocalDate())
            .isEqualTo(LocalDate.parse(currentDate(), DateTimeFormatter.BASIC_ISO_DATE));
    assertThat(savedCandles.get(0).getAt().toLocalTime().format(DateTimeFormatter.ofPattern("HHmmss")))
            .isEqualTo("153000");
  }

  @Test
  @DisplayName("잘못된 날짜 데이터는 건너뛰고 정상 분봉만 저장한다")
  void initializeIndexMinuteCandles_skipInvalidDate() {
    // Given
    MarketIndexType indexType = MarketIndexType.KOSPI;
    Stock indexStock = Stock.builder()
            .id(1L)
            .name("코스피")
            .symbol("INDEX_KOSPI")
            .build();

    when(stockRepository.findBySymbol("INDEX_KOSPI")).thenReturn(Optional.of(indexStock));
    when(priceCandleRepository.existsByStockIdAndTimeframe(1L, Timeframe.MINUTE)).thenReturn(false);

    KisDto.IndexTimePriceOutput invalidDate = createIndexOutput("999999", "153000", "2500.50", "100", "1000000");
    KisDto.IndexTimePriceOutput validDate = createIndexOutput("152000", "2501.00", "110", "1100000");

    when(kisApiClient.fetchIndexTimePrice(eq(KisApiClient.IndexCode.KOSPI), eq("60")))
            .thenReturn(List.of(invalidDate, validDate));

    when(priceCandleRepository.findByStockIdAndTimeframeAndAtIn(eq(1L), eq(Timeframe.MINUTE), anyList()))
            .thenReturn(Collections.emptyList());

    // When
    service.initializeIndexMinuteCandlesIfEmpty(indexType);

    // Then
    ArgumentCaptor<List<PriceCandle>> captor = ArgumentCaptor.forClass(List.class);
    verify(priceCandleRepository).saveAll(captor.capture());
    List<PriceCandle> savedCandles = captor.getValue();

    assertThat(savedCandles).hasSize(1);
    assertThat(savedCandles.get(0).getAt().toLocalTime().format(DateTimeFormatter.ofPattern("HHmmss")))
            .isEqualTo("152000");
  }

  private KisDto.IndexTimePriceOutput createIndexOutput(String time, String price, String volume, String value) {
    return createIndexOutput(currentDate(), time, price, volume, value);
  }

  private KisDto.IndexTimePriceOutput createIndexOutput(
          String date,
          String time,
          String price,
          String volume,
          String value
  ) {
    return KisDto.IndexTimePriceOutput.builder()
            .stck_bsop_date(date)
            .stck_cntg_hour(time)
            .bsop_hour(time)
            .bstp_nmix_prpr(price)
            .bstp_nmix_oprc(price)
            .bstp_nmix_hgpr(price)
            .bstp_nmix_lwpr(price)
            .bstp_nmix_prdy_ctrt("0.5")
            .cntg_vol(volume)
            .acml_tr_pbmn(value)
            .build();
  }

  private String currentDate() {
    return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
  }
}
