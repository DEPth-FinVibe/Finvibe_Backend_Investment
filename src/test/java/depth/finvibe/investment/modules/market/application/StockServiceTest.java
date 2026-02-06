package depth.finvibe.investment.modules.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import depth.finvibe.investment.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.StockRankingRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockThemeRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.StockRanking;
import depth.finvibe.investment.modules.market.domain.enums.RankType;
import depth.finvibe.investment.modules.market.dto.StockDto;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StockThemeRepository stockThemeRepository;

    @Mock
    private RealMarketClient realMarketClient;

    @Mock
    private StockRankingRepository stockRankingRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("bulkUpsertStocks는 테마에 맞는 카테고리를 사용한다")
    void bulkUpsertStocks_usesMatchingThemeCategory() {
        Category fallbackCategory = Category.builder()
                .id(1L)
                .name("기타")
                .build();
        Category matchCategory = Category.builder()
                .id(2L)
                .name("반도체")
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(fallbackCategory, matchCategory));
        when(stockThemeRepository.findSymbolToThemeMap()).thenReturn(Map.of("1234ACM", "반도체"));

        StockDto.RealMarketStockResponse request = StockDto.RealMarketStockResponse.builder()
                .name("Acme")
                .symbol("1234ACM")
                .build();
        when(realMarketClient.fetchStocksInRealMarket()).thenReturn(List.of(request));

        stockService.bulkUpsertStocks();

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).bulkUpsertStocks(captor.capture());
        List<Stock> stocks = captor.getValue();

        assertThat(stocks).hasSize(1);
        Stock stock = stocks.get(0);
        assertThat(stock.getName()).isEqualTo("Acme");
        assertThat(stock.getSymbol()).isEqualTo("1234ACM");
        assertThat(stock.getCategoryId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("bulkUpsertStocks는 테마가 없으면 기타 카테고리를 사용한다")
    void bulkUpsertStocks_fallsBackToOtherCategory() {
        Category fallbackCategory = Category.builder()
                .id(10L)
                .name("기타")
                .build();
        Category otherCategory = Category.builder()
                .id(20L)
                .name("전기차")
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(fallbackCategory, otherCategory));
        when(stockThemeRepository.findSymbolToThemeMap()).thenReturn(Map.of("9999BET", "없는테마"));

        StockDto.RealMarketStockResponse request = StockDto.RealMarketStockResponse.builder()
                .name("Beta")
                .symbol("9999BET")
                .build();
        when(realMarketClient.fetchStocksInRealMarket()).thenReturn(List.of(request));

        stockService.bulkUpsertStocks();

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).bulkUpsertStocks(captor.capture());
        List<Stock> stocks = captor.getValue();

        assertThat(stocks).hasSize(1);
        Stock stock = stocks.get(0);
        assertThat(stock.getCategoryId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("bulkUpsertStocks는 기타 카테고리가 없으면 예외를 던진다")
    void bulkUpsertStocks_throwsWhenFallbackCategoryMissing() {
        Category otherCategory = Category.builder()
                .id(20L)
                .name("반도체")
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(otherCategory));
        when(stockThemeRepository.findSymbolToThemeMap()).thenReturn(Map.of("9999GAM", "없는테마"));

        StockDto.RealMarketStockResponse request = StockDto.RealMarketStockResponse.builder()
                .name("Gamma")
                .symbol("9999GAM")
                .build();
        when(realMarketClient.fetchStocksInRealMarket()).thenReturn(List.of(request));

        assertThatThrownBy(() -> stockService.bulkUpsertStocks())
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(stockRepository);
    }

    @Test
    @DisplayName("renewStockCharts는 동일 종목/순위타입 중복 데이터를 하나로 병합한다")
    void renewStockCharts_deduplicatesByStockIdAndRankType() {
        StockDto.RankingResponse first = StockDto.RankingResponse.builder()
                .symbol("005930")
                .rankType(RankType.TOP_VALUE)
                .rank(2)
                .build();
        StockDto.RankingResponse second = StockDto.RankingResponse.builder()
                .symbol("005930")
                .rankType(RankType.TOP_VALUE)
                .rank(1)
                .build();
        when(realMarketClient.fetchStockRankings()).thenReturn(List.of(first, second));

        Stock stock = Stock.builder()
                .id(5474L)
                .name("삼성전자")
                .symbol("005930")
                .categoryId(10L)
                .build();
        when(stockRepository.findAllBySymbolIn(anyList())).thenReturn(List.of(stock));

        stockService.renewStockCharts();

        verify(stockRankingRepository).deleteByRankTypeIn(List.of(RankType.TOP_VALUE));

        ArgumentCaptor<List<StockRanking>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRankingRepository).bulkUpsertStockRankings(captor.capture());
        List<StockRanking> savedRankings = captor.getValue();

        assertThat(savedRankings).hasSize(1);
        assertThat(savedRankings.get(0).getStockId()).isEqualTo(5474L);
        assertThat(savedRankings.get(0).getRankType()).isEqualTo(RankType.TOP_VALUE);
        assertThat(savedRankings.get(0).getRank()).isEqualTo(1);
    }
}
