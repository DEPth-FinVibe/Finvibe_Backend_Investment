package depth.finvibe.investment.modules.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import depth.finvibe.investment.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.dto.StockDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RealMarketClient realMarketClient;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("bulkUpsertStocks는 일치하는 카테고리 코드를 사용한다")
    void bulkUpsertStocks_usesMatchingCategoryCode() {
        Category fallbackCategory = Category.builder()
                .id(1L)
                .name("Fallback")
                .code("0000")
                .build();
        Category matchCategory = Category.builder()
                .id(2L)
                .name("Tech")
                .code("1234")
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(fallbackCategory, matchCategory));

        StockDto.RealMarketResponse request = StockDto.RealMarketResponse.builder()
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
    @DisplayName("bulkUpsertStocks는 일치하는 카테고리가 없으면 기본 카테고리를 사용한다")
    void bulkUpsertStocks_fallsBackToDefaultCategory() {
        Category fallbackCategory = Category.builder()
                .id(10L)
                .name("Fallback")
                .code("0000")
                .build();
        Category otherCategory = Category.builder()
                .id(20L)
                .name("Other")
                .code("5678")
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(fallbackCategory, otherCategory));

        StockDto.RealMarketResponse request = StockDto.RealMarketResponse.builder()
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
    @DisplayName("bulkUpsertStocks는 기본 카테고리가 없으면 예외를 던진다")
    void bulkUpsertStocks_throwsWhenFallbackCategoryMissing() {
        Category otherCategory = Category.builder()
                .id(20L)
                .name("Other")
                .code("5678")
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(otherCategory));

        StockDto.RealMarketResponse request = StockDto.RealMarketResponse.builder()
                .name("Gamma")
                .symbol("9999GAM")
                .build();
        when(realMarketClient.fetchStocksInRealMarket()).thenReturn(List.of(request));

        assertThatThrownBy(() -> stockService.bulkUpsertStocks())
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(stockRepository);
    }
}
