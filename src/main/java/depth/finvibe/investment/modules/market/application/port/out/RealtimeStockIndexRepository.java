package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.RealtimeStockIndex;
import java.util.List;

public interface RealtimeStockIndexRepository {
    void addRealtimeStockIndex(RealtimeStockIndex realtimeStockIndex);
    void renewRealtimeStockIndex(RealtimeStockIndex realtimeStockIndex);
    void removeRealtimeStockIndex(RealtimeStockIndex realtimeStockIndex);

    boolean existsByStockId(Long stockId);
    boolean allExistsByStockIds(Iterable<Long> stockIds);

    List<Long> findActiveStockIds();
}