package depth.finvibe.investment.modules.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import depth.finvibe.investment.modules.market.application.port.out.BatchUpdatePriceRepository;
import depth.finvibe.investment.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import depth.finvibe.investment.modules.market.dto.CategoryInternalDto;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private BatchUpdatePriceRepository batchUpdatePriceRepository;

    @InjectMocks
    private CategoryQueryService categoryQueryService;

    @Test
    @DisplayName("내부 카테고리 목록 조회는 저장소 조회 순서를 유지한다")
    void getAllCategoriesForInternal_keepsRepositoryOrder() {
        Category second = Category.builder()
                .id(2L)
                .name("반도체")
                .build();
        Category first = Category.builder()
                .id(1L)
                .name("2차전지")
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(second, first));

        List<CategoryInternalDto.Response> result = categoryQueryService.getAllCategoriesForInternal();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryId()).isEqualTo(2L);
        assertThat(result.get(0).getCategoryName()).isEqualTo("반도체");
        assertThat(result.get(1).getCategoryId()).isEqualTo(1L);
        assertThat(result.get(1).getCategoryName()).isEqualTo("2차전지");
        verifyNoInteractions(stockRepository, batchUpdatePriceRepository);
    }

    @Test
    @DisplayName("내부 카테고리 목록 조회는 데이터가 없으면 빈 목록을 반환한다")
    void getAllCategoriesForInternal_returnsEmptyListWhenNoCategory() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryInternalDto.Response> result = categoryQueryService.getAllCategoriesForInternal();

        assertThat(result).isEmpty();
        verifyNoInteractions(stockRepository, batchUpdatePriceRepository);
    }
}
