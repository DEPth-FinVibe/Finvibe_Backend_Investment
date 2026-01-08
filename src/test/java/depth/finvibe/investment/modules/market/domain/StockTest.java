package depth.finvibe.investment.modules.market.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StockTest {

    @Test
    @DisplayName("종목 생성 시 정보가 설정된다")
    void createStock() {
        // given & when
        Stock stock = Stock.builder()
                .name("삼성전자")
                .symbol("005930")
                .categoryId(1L)
                .build();

        // then
        assertThat(stock.getName()).isEqualTo("삼성전자");
        assertThat(stock.getSymbol()).isEqualTo("005930");
        assertThat(stock.getCategoryId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("종목 정보를 업데이트한다")
    void updateInfo() {
        // given
        Stock stock = Stock.builder()
                .name("삼성전자")
                .symbol("005930")
                .categoryId(1L)
                .build();

        // when
        stock.updateInfo("삼성전자 우선주", "005935", 2L);

        // then
        assertThat(stock.getName()).isEqualTo("삼성전자 우선주");
        assertThat(stock.getSymbol()).isEqualTo("005935");
        assertThat(stock.getCategoryId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("카테고리를 변경한다")
    void changeCategory() {
        // given
        Stock stock = Stock.builder()
                .name("삼성전자")
                .symbol("005930")
                .categoryId(1L)
                .build();

        // when
        stock.changeCategory(2L);

        // then
        assertThat(stock.getCategoryId()).isEqualTo(2L);
    }
}