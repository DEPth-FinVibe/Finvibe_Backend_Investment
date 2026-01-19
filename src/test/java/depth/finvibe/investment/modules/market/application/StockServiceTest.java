package depth.finvibe.investment.modules.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import depth.finvibe.investment.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.dto.StockDto;
import java.util.List;
import java.util.NoSuchElementException;
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

        StockDto.CreateRequest request = StockDto.CreateRequest.builder()
                .name("Acme")
                .symbol("ACM")
                .rawCategoryCode("1234")
                .build();

        stockService.bulkUpsertStocks(List.of(request));

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockRepository).bulkUpsertStocks(captor.capture());
        List<Stock> stocks = captor.getValue();

        assertThat(stocks).hasSize(1);
        Stock stock = stocks.get(0);
        assertThat(stock.getName()).isEqualTo("Acme");
        assertThat(stock.getSymbol()).isEqualTo("ACM");
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

        StockDto.CreateRequest request = StockDto.CreateRequest.builder()
                .name("Beta")
                .symbol("BET")
                .rawCategoryCode("9999")
                .build();

        stockService.bulkUpsertStocks(List.of(request));

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

        StockDto.CreateRequest request = StockDto.CreateRequest.builder()
                .name("Gamma")
                .symbol("GAM")
                .rawCategoryCode("5678")
                .build();

        assertThatThrownBy(() -> stockService.bulkUpsertStocks(List.of(request)))
                .isInstanceOf(NoSuchElementException.class);
        verifyNoInteractions(stockRepository);
    }
}
