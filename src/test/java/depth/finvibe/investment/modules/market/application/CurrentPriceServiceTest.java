package depth.finvibe.investment.modules.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.HoldingStockRepository;
import depth.finvibe.investment.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.RealtimeStockIndexRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.PriceCandle;
import depth.finvibe.investment.modules.market.domain.RealtimeStockIndex;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.enums.Timeframe;
import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.modules.market.dto.CurrentPriceUpdatedEvent;
import depth.finvibe.investment.modules.market.dto.PriceCandleDto;
import depth.finvibe.investment.modules.market.dto.StockDto;
import depth.finvibe.investment.shared.error.DomainException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CurrentPriceServiceTest {

    @Mock
    private PriceCandleRepository priceCandleRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private RealMarketClient realMarketClient;

    @Mock
    private HoldingStockRepository holdingStockRepository;

    @Mock
    private RealtimeStockIndexRepository realtimeStockIndexRepository;

    @Mock
    private CurrentPriceRepository currentPriceRepository;

    @InjectMocks
    private CurrentPriceService currentPriceService;

    @Test
    @DisplayName("registerWatchingStock은 종목이 존재하면 인덱스를 등록한다")
    void registerWatchingStock_addsIndexWhenStockExists() {
        Long stockId = 10L;
        UUID userId = UUID.randomUUID();
        when(stockRepository.existsById(stockId)).thenReturn(true);

        currentPriceService.registerWatchingStock(stockId, userId);

        ArgumentCaptor<RealtimeStockIndex> indexCaptor = ArgumentCaptor.forClass(RealtimeStockIndex.class);
        verify(realtimeStockIndexRepository).addRealtimeStockIndex(indexCaptor.capture());
        RealtimeStockIndex savedIndex = indexCaptor.getValue();
        assertThat(savedIndex.getStockId()).isEqualTo(stockId);
        assertThat(savedIndex.getWatcherId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("registerWatchingStock은 종목이 없으면 예외를 던진다")
    void registerWatchingStock_throwsWhenStockMissing() {
        Long stockId = 999L;
        UUID userId = UUID.randomUUID();
        when(stockRepository.existsById(stockId)).thenReturn(false);

        assertThatThrownBy(() -> currentPriceService.registerWatchingStock(stockId, userId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", MarketErrorCode.STOCK_NOT_FOUND);
        verifyNoInteractions(realtimeStockIndexRepository);
    }

    @Test
    @DisplayName("stockPriceUpdated는 인덱스에 없으면 갱신을 건너뛴다")
    void stockPriceUpdated_skipsWhenNotIndexed() {
        CurrentPriceUpdatedEvent event = CurrentPriceUpdatedEvent.builder()
                .stockId(1L)
                .at(LocalDateTime.now())
                .close(BigDecimal.TEN)
                .open(BigDecimal.ONE)
                .high(BigDecimal.TEN)
                .low(BigDecimal.ONE)
                .prevDayChangePct(BigDecimal.ZERO)
                .volume(BigDecimal.ZERO)
                .value(BigDecimal.ZERO)
                .build();
        when(realtimeStockIndexRepository.existsByStockId(1L)).thenReturn(false);

        currentPriceService.stockPriceUpdated(event);

        verifyNoInteractions(currentPriceRepository);
    }

    @Test
    @DisplayName("getStockCandles는 누락된 캔들을 조회/저장하고 정렬한다")
    void getStockCandles_fetchesMissingSavesAndSorts_full() {
        Long stockId = 1L;
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 5, 30);
        Timeframe timeframe = Timeframe.MINUTE;
        int count = 2;

        when(priceCandleRepository.findExisting(stockId, startTime, timeframe, count))
                .thenReturn(List.of());

        PriceCandleDto.Response later = candleResponse(stockId, timeframe,
                LocalDateTime.of(2024, 1, 1, 10, 6, 0));
        PriceCandleDto.Response earlier = candleResponse(stockId, timeframe,
                LocalDateTime.of(2024, 1, 1, 10, 5, 0));

        when(realMarketClient.fetchPriceCandles(eq(stockId), anyList(), eq(timeframe)))
                .thenReturn(List.of(later, earlier));

        List<PriceCandleDto.Response> result =
                currentPriceService.getStockCandles(stockId, startTime, timeframe, count);

        ArgumentCaptor<List<LocalDateTime>> timesCaptor = ArgumentCaptor.forClass(List.class);
        verify(realMarketClient).fetchPriceCandles(eq(stockId), timesCaptor.capture(), eq(timeframe));
        Set<LocalDateTime> requestedTimes = Set.copyOf(timesCaptor.getValue());
        assertThat(requestedTimes).containsExactlyInAnyOrder(
                LocalDateTime.of(2024, 1, 1, 10, 5, 0),
                LocalDateTime.of(2024, 1, 1, 10, 6, 0)
        );

        ArgumentCaptor<List<PriceCandle>> candlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(priceCandleRepository).saveAll(candlesCaptor.capture());
        List<PriceCandle> saved = candlesCaptor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getStockId()).isEqualTo(stockId);
        assertThat(saved.get(0).getTimeframe()).isEqualTo(timeframe);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 5, 0));
        assertThat(result.get(1).getAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 6, 0));
    }

    @Test
    @DisplayName("getCurrentPrices는 모든 종목이 인덱스에 없으면 예외를 던진다")
    void getCurrentPrices_throwsWhenNotAllIndexed() {
        List<Long> stockIds = List.of(1L, 2L);
        when(realtimeStockIndexRepository.allExistsByStockIds(stockIds)).thenReturn(false);

        assertThatThrownBy(() -> currentPriceService.getCurrentPrices(stockIds))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("errorCode", MarketErrorCode.STOCK_NOT_FOUND);
        verifyNoInteractions(currentPriceRepository);
    }

    @Test
    @DisplayName("getTopStocksByValue는 응답 순서대로 종목을 매핑한다")
    void getTopStocksByValue_mapsResponseOrder() {
        StockDto.TopStockResponse topA = StockDto.TopStockResponse.builder().symbol("AAA").build();
        StockDto.TopStockResponse topB = StockDto.TopStockResponse.builder().symbol("BBB").build();
        when(realMarketClient.getTopStocksByValue(100)).thenReturn(List.of(topA, topB));

        Stock stockA = Stock.builder().id(1L).name("A").symbol("AAA").categoryId(10L).build();
        Stock stockB = Stock.builder().id(2L).name("B").symbol("BBB").categoryId(20L).build();
        when(stockRepository.findAllBySymbolIn(List.of("AAA", "BBB")))
                .thenReturn(List.of(stockB, stockA));

        List<StockDto.Response> result = currentPriceService.getTopStocksByValue(Pageable.unpaged());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSymbol()).isEqualTo("AAA");
        assertThat(result.get(0).getStockId()).isEqualTo(1L);
        assertThat(result.get(1).getSymbol()).isEqualTo("BBB");
        assertThat(result.get(1).getStockId()).isEqualTo(2L);
    }

    private PriceCandleDto.Response candleResponse(Long stockId, Timeframe timeframe, LocalDateTime at) {
        return PriceCandleDto.Response.builder()
                .stockId(stockId)
                .timeframe(timeframe)
                .at(at)
                .open(BigDecimal.ONE)
                .close(BigDecimal.TEN)
                .high(BigDecimal.TEN)
                .low(BigDecimal.ONE)
                .volume(BigDecimal.valueOf(100))
                .value(BigDecimal.valueOf(1000))
                .prevDayChangePct(BigDecimal.ZERO)
                .build();
    }
}
