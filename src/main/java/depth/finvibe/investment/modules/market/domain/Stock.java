package depth.finvibe.investment.modules.market.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal totalHoldingAmount = BigDecimal.ZERO;

    public static Stock create(String name, String symbol, Long categoryId) {
        return Stock.builder()
                .name(name)
                .symbol(symbol)
                .categoryId(categoryId)
                .totalHoldingAmount(BigDecimal.ZERO)
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

    public void updateTotalHoldingAmount(BigDecimal totalHoldingAmount) {
        this.totalHoldingAmount = totalHoldingAmount;
    }

}
