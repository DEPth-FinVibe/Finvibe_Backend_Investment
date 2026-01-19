package depth.finvibe.investment.modules.market.infra.redis;

import depth.finvibe.investment.modules.market.application.port.out.RealtimeStockIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RealtimeStockIndexRepositoryImpl implements RealtimeStockIndexRepository {

    @Override
    public void addRealtimeStockIndex(Long stockId, UUID watcherId) {

    }

    @Override
    public void renewRealtimeStockIndex(Long stockId, UUID watcherId) {

    }

    @Override
    public void removeRealtimeStockIndex(Long stockId, UUID watcherId) {

    }

    @Override
    public boolean existsByStockId(Long stockId) {
        return false;
    }

    @Override
    public boolean allExistsByStockIds(Iterable<Long> stockIds) {
        return false;
    }
}