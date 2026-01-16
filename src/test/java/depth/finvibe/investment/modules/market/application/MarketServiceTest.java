package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.out.*;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.dto.CurrentPriceDto;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @Mock
    private PriceCandleRepository priceCandleRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private CurrentPriceRepository currentPriceRepository;

    @Mock
    private PriceUpdatePublisher priceUpdatePublisher;

    @Mock
    private PriceUpdateSubscriber priceUpdateSubscriber;

    @Mock
    private LeadershipLock leadershipLock;

    @Mock
    private RegionOfInterestRepository regionOfInterestRepository;

    @InjectMocks
    private MarketService marketService;

    @Test
    @DisplayName("특정 종목의 캔들 데이터 조회 - 성공")
    void getStockCandles_Success() {
        // given
        Long stockId = 1L;
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        Timeframe timeframe = Timeframe.DAY;

        List<PriceCandle> candles = Arrays.asList(
                createPriceCandle(stockId, Timeframe.DAY, LocalDateTime.now().minusDays(1)),
                createPriceCandle(stockId, Timeframe.DAY, LocalDateTime.now().minusDays(2))
        );

        given(priceCandleRepository.findByStockIdAndTimeframeOrderByAtDesc(stockId, startTime, endTime, timeframe))
                .willReturn(candles);

        // when
        List<PriceCandleDto.Response> responses = marketService.getStockCandles(stockId, startTime, endTime, timeframe);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStockId()).isEqualTo(stockId);
        assertThat(responses.get(0).getTimeframe()).isEqualTo(Timeframe.DAY);
        verify(priceCandleRepository, times(1))
                .findByStockIdAndTimeframeOrderByAtDesc(stockId, startTime, endTime, timeframe);
    }

    @Test
    @DisplayName("특정 종목의 캔들 데이터 조회 - 결과 없음")
    void getStockCandles_NoResults() {
        // given
        Long stockId = 999L;
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        Timeframe timeframe = Timeframe.DAY;

        given(priceCandleRepository.findByStockIdAndTimeframeOrderByAtDesc(stockId, startTime, endTime, timeframe))
                .willReturn(List.of());

        // when
        List<PriceCandleDto.Response> responses = marketService.getStockCandles(stockId, startTime, endTime, timeframe);

        // then
        assertThat(responses).isEmpty();
        verify(priceCandleRepository, times(1))
                .findByStockIdAndTimeframeOrderByAtDesc(stockId, startTime, endTime, timeframe);
    }

    @Test
    @DisplayName("여러 종목의 현재가 조회 - 캐시 히트")
    void getCurrentPrices_CacheHit() {
        // given
        List<Long> stockIds = Arrays.asList(1L, 2L);
        List<CurrentPrice> prices = Arrays.asList(
                createCurrentPrice(1L),
                createCurrentPrice(2L)
        );

        given(currentPriceRepository.findByStockIds(stockIds))
                .willReturn(prices);

        // when
        List<CurrentPriceDto.Response> responses = marketService.getCurrentPrices(stockIds);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getStockId()).isEqualTo(1L);
        assertThat(responses.get(1).getStockId()).isEqualTo(2L);
        verify(currentPriceRepository, times(1)).findByStockIds(stockIds);
        verify(priceCandleRepository, never()).findLatestForEachStock(anyList(), any(Timeframe.class));
    }


    @Test
    @DisplayName("여러 종목의 현재가 조회 - 캐시 미스")
    void getCurrentPrices_CacheMiss() {
        // given
        List<Long> stockIds = Arrays.asList(1L, 2L, 3L);
        List<CurrentPrice> cachedPrices = new ArrayList<>(List.of(createCurrentPrice(1L)));
        List<PriceCandle> fallbackCandles = Arrays.asList(
                createPriceCandle(2L, Timeframe.DAY, LocalDateTime.now()),
                createPriceCandle(3L, Timeframe.DAY, LocalDateTime.now())
        );

        given(currentPriceRepository.findByStockIds(stockIds))
                .willReturn(cachedPrices);
        given(priceCandleRepository.findLatestForEachStock(Arrays.asList(2L, 3L), Timeframe.DAY))
                .willReturn(fallbackCandles);

        // when
        List<CurrentPriceDto.Response> responses = marketService.getCurrentPrices(stockIds);

        // then
        assertThat(responses).hasSize(3);
        verify(currentPriceRepository, times(1)).findByStockIds(stockIds);
        verify(priceCandleRepository, times(1)).findLatestForEachStock(anyList(), eq(Timeframe.DAY));
        verify(currentPriceRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("거래대금 TOP100 조회 - 성공")
    void getTopStocksByValue_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Stock> stocks = Arrays.asList(
                createStock(1L, "AAPL", 1L),
                createStock(2L, "GOOGL", 1L)
        );
        Page<Stock> stockPage = new PageImpl<>(stocks, pageable, stocks.size());

        given(stockRepository.findTop100ByOrderByCurrentValueDesc(pageable))
                .willReturn(stockPage);

        // when
        Page<StockDto.Response> responses = marketService.getTopStocksByValue(pageable);

        // then
        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent().get(0).getStockId()).isEqualTo(1L);
        assertThat(responses.getContent().get(0).getName()).isEqualTo("AAPL");
        verify(stockRepository, times(1)).findTop100ByOrderByCurrentValueDesc(pageable);
    }

    @Test
    @DisplayName("거래량 TOP100 조회 - 성공")
    void getTopStocksByVolume_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Stock> stocks = Arrays.asList(createStock(1L, "AAPL", 1L));
        Page<Stock> stockPage = new PageImpl<>(stocks, pageable, stocks.size());

        given(stockRepository.findTop100ByOrderByCurrentVolumeDesc(pageable))
                .willReturn(stockPage);

        // when
        Page<StockDto.Response> responses = marketService.getTopStocksByVolume(pageable);

        // then
        assertThat(responses.getContent()).hasSize(1);
        verify(stockRepository, times(1)).findTop100ByOrderByCurrentVolumeDesc(pageable);
    }

    @Test
    @DisplayName("급상승 TOP100 조회 - 성공")
    void getTopRisingStocks_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Stock> stocks = Arrays.asList(createStock(1L, "TSLA", 1L));
        Page<Stock> stockPage = new PageImpl<>(stocks, pageable, stocks.size());

        given(stockRepository.findTop100ByOrderByPrevDayChangePctDesc(pageable))
                .willReturn(stockPage);

        // when
        Page<StockDto.Response> responses = marketService.getTopRisingStocks(pageable);

        // then
        assertThat(responses.getContent()).hasSize(1);
        verify(stockRepository, times(1)).findTop100ByOrderByPrevDayChangePctDesc(pageable);
    }

    @Test
    @DisplayName("급하락 TOP100 조회 - 성공")
    void getTopFallingStocks_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Stock> stocks = Arrays.asList(createStock(1L, "META", 1L));
        Page<Stock> stockPage = new PageImpl<>(stocks, pageable, stocks.size());

        given(stockRepository.findTop100ByOrderByPrevDayChangePctAsc(pageable))
                .willReturn(stockPage);

        // when
        Page<StockDto.Response> responses = marketService.getTopFallingStocks(pageable);

        // then
        assertThat(responses.getContent()).hasSize(1);
        verify(stockRepository, times(1)).findTop100ByOrderByPrevDayChangePctAsc(pageable);
    }

    @Test
    @DisplayName("현재가 업데이트 - 성공")
    void updateCurrentPrices_Success() {
        // given
        List<Long> stockIds = Arrays.asList(1L, 2L);
        List<PriceCandle> candles = Arrays.asList(
                createPriceCandle(1L, Timeframe.DAY, LocalDateTime.now()),
                createPriceCandle(2L, Timeframe.DAY, LocalDateTime.now())
        );

        given(priceCandleRepository.findLatestForEachStock(stockIds, Timeframe.DAY))
                .willReturn(candles);
        doNothing().when(priceUpdatePublisher).publishBulkPriceUpdate(anyList());

        // when
        marketService.updateCurrentPrices(stockIds);

        // then
        verify(priceCandleRepository, times(1)).findLatestForEachStock(stockIds, Timeframe.DAY);
        verify(currentPriceRepository, times(1)).saveAll(anyList());
        verify(priceUpdatePublisher, times(1)).publishBulkPriceUpdate(anyList());
    }

    @Test
    @DisplayName("현재가 업데이트 - WebSocket 발행 실패")
    void updateCurrentPrices_PublishFailed() {
        // given
        List<Long> stockIds = Arrays.asList(1L);
        List<PriceCandle> candles = Arrays.asList(
                createPriceCandle(1L, Timeframe.DAY, LocalDateTime.now())
        );

        given(priceCandleRepository.findLatestForEachStock(stockIds, Timeframe.DAY))
                .willReturn(candles);
        doThrow(new RuntimeException("WebSocket error"))
                .when(priceUpdatePublisher).publishBulkPriceUpdate(anyList());

        // when & then
        assertThatCode(() -> marketService.updateCurrentPrices(stockIds))
                .doesNotThrowAnyException();

        verify(currentPriceRepository, times(1)).saveAll(anyList());
        verify(priceUpdatePublisher, times(1)).publishBulkPriceUpdate(anyList());
    }

    @Test
    @DisplayName("ROI Level 1 추가 - 성공")
    void addRegionOfInterestLevel1_Success() {
        // given
        List<Long> stockIds = Arrays.asList(1L, 2L, 3L);

        // when
        marketService.addRegionOfInterestLevel1(stockIds);

        // then
        verify(regionOfInterestRepository, times(3)).addToLevel1(anyLong());
        verify(regionOfInterestRepository, times(1)).addToLevel1(1L);
        verify(regionOfInterestRepository, times(1)).addToLevel1(2L);
        verify(regionOfInterestRepository, times(1)).addToLevel1(3L);
    }

    @Test
    @DisplayName("ROI Level 2 추가 - 성공")
    void addRegionOfInterestLevel2_Success() {
        // given
        List<Long> stockIds = Arrays.asList(1L, 2L);

        // when
        marketService.addRegionOfInterestLevel2(stockIds);

        // then
        verify(regionOfInterestRepository, times(2)).addToLevel2(anyLong());
        verify(regionOfInterestRepository, times(1)).addToLevel2(1L);
        verify(regionOfInterestRepository, times(1)).addToLevel2(2L);
    }

    @Test
    @DisplayName("ROI Level 1 제거 - 성공")
    void removeRegionOfInterestLevel1_Success() {
        // given
        List<Long> stockIds = Arrays.asList(1L, 2L);

        // when
        marketService.removeRegionOfInterestLevel1(stockIds);

        // then
        verify(regionOfInterestRepository, times(2)).removeFromLevel1(anyLong());
        verify(regionOfInterestRepository, times(1)).removeFromLevel1(1L);
        verify(regionOfInterestRepository, times(1)).removeFromLevel1(2L);
    }

    @Test
    @DisplayName("ROI Level 2 제거 - 성공")
    void removeRegionOfInterestLevel2_Success() {
        // given
        List<Long> stockIds = Arrays.asList(1L);

        // when
        marketService.removeRegionOfInterestLevel2(stockIds);

        // then
        verify(regionOfInterestRepository, times(1)).removeFromLevel2(anyLong());
        verify(regionOfInterestRepository, times(1)).removeFromLevel2(1L);
    }

    @Test
    @DisplayName("ROI Level 1 추가 - 빈 리스트")
    void addRegionOfInterestLevel1_EmptyList() {
        // given
        List<Long> stockIds = List.of();

        // when
        marketService.addRegionOfInterestLevel1(stockIds);

        // then
        verify(regionOfInterestRepository, never()).addToLevel1(anyLong());
    }

    @Test
    @DisplayName("신규 종목 등록 - 심볼 없음")
    void registerNewStock_MissingSymbol() {
        // given
        StockDto.NewStock request = StockDto.NewStock.builder()
                .name("Alpha")
                .categoryId(1L)
                .build();

        // when
        marketService.registerNewStock(request);

        // then
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("신규 종목 등록 - 기존 종목 업데이트")
    void registerNewStock_ExistingStock() {
        // given
        Stock existing = Stock.create("OldName", "AAA", 1L);
        given(stockRepository.findBySymbol("AAA"))
                .willReturn(Optional.of(existing));

        StockDto.NewStock request = StockDto.NewStock.builder()
                .name("NewName")
                .symbol("AAA")
                .categoryId(2L)
                .build();

        // when
        marketService.registerNewStock(request);

        // then
        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository, times(1)).save(captor.capture());
        Stock saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getName()).isEqualTo("NewName");
        assertThat(saved.getSymbol()).isEqualTo("AAA");
        assertThat(saved.getCategoryId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("신규 종목 등록 - 신규 생성")
    void registerNewStock_NewStock() {
        // given
        given(stockRepository.findBySymbol("BBB"))
                .willReturn(Optional.empty());

        StockDto.NewStock request = StockDto.NewStock.builder()
                .name("BrandNew")
                .symbol("BBB")
                .categoryId(3L)
                .build();

        // when
        marketService.registerNewStock(request);

        // then
        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository, times(1)).save(captor.capture());
        Stock saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("BrandNew");
        assertThat(saved.getSymbol()).isEqualTo("BBB");
        assertThat(saved.getCategoryId()).isEqualTo(3L);
    }

    // 테스트 헬퍼 메서드
    private PriceCandle createPriceCandle(Long stockId, Timeframe timeframe, LocalDateTime at) {
        return PriceCandle.builder()
                .stockId(stockId)
                .timeframe(timeframe)
                .at(at)
                .open(BigDecimal.valueOf(100.0))
                .high(BigDecimal.valueOf(105.0))
                .low(BigDecimal.valueOf(95.0))
                .close(BigDecimal.valueOf(102.0))
                .prevDayChangePct(BigDecimal.valueOf(2.0))
                .volume(BigDecimal.valueOf(1000000))
                .value(BigDecimal.valueOf(102000000))
                .build();
    }

    private CurrentPrice createCurrentPrice(Long stockId) {
        return new CurrentPrice(
                stockId,
                LocalDateTime.now(),
                BigDecimal.valueOf(102.0),
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(105.0),
                BigDecimal.valueOf(95.0),
                BigDecimal.valueOf(102.0),
                BigDecimal.valueOf(2.0),
                BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(102000000)
        );
    }

    private Stock createStock(Long id, String name, Long categoryId) {
        return Stock.builder()
                .id(id)
                .name(name)
                .categoryId(categoryId)
                .build();
    }
}
