package depth.finvibe.investment.modules.asset.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "user_profit_ranking",
    indexes = {
        @Index(name = "idx_user_profit_ranking_rank", columnList = "rank"),
        @Index(name = "idx_user_profit_ranking_user_id", columnList = "user_id")
    }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserProfitRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "total_return_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalReturnRate;

    @Column(name = "total_profit_loss", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalProfitLoss;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserProfitRanking create(
        UUID userId,
        BigDecimal totalReturnRate,
        BigDecimal totalProfitLoss,
        Integer rank
    ) {
        return UserProfitRanking.builder()
            .userId(userId)
            .totalReturnRate(totalReturnRate)
            .totalProfitLoss(totalProfitLoss)
            .rank(rank)
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
