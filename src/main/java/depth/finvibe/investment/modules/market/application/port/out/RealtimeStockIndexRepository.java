package depth.finvibe.investment.modules.market.application.port.out;

import java.util.UUID;

public interface RealtimeStockIndexRepository {
    void addRealtimeStockIndex(Long stockId, UUID watcherId);
    void renewRealtimeStockIndex(Long stockId, UUID watcherId);
    void removeRealtimeStockIndex(Long stockId, UUID watcherId);

    boolean existsByStockId(Long stockId);
    boolean allExistsByStockIds(Iterable<Long> stockIds);
}