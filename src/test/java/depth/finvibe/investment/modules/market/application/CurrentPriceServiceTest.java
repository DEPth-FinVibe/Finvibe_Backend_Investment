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
