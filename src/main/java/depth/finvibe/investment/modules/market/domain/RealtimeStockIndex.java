package depth.finvibe.investment.modules.market.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Builder
@EqualsAndHashCode
public class RealtimeStockIndex {
    private final Long stockId;
    private final UUID watcherId;

    public static RealtimeStockIndex create(Long stockId, UUID watcherId) {
        return RealtimeStockIndex.builder()
                .stockId(stockId)
                .watcherId(watcherId)
                .build();
    }
}
