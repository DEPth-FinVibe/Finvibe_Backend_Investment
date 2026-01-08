package depth.finvibe.investment.modules.market.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Stock {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String symbol;

    private Long categoryId;

    public static Stock create(String name, String symbol, Long categoryId) {
        return Stock.builder()
                .name(name)
                .symbol(symbol)
                .categoryId(categoryId)
                .build();
    }

    // 종목 정보 업데이트
    public void updateInfo(String name, String symbol, Long categoryId) {
        this.name = name;
        this.symbol = symbol;
        this.categoryId = categoryId;
    }

    // 카테고리 변경
    public void changeCategory(Long categoryId) {
        this.categoryId = categoryId;
    }

}
