package depth.finvibe.investment.modules.market.infra.redis;

import depth.finvibe.investment.modules.market.application.port.out.RealtimeStockIndexRepository;
import depth.finvibe.investment.modules.market.domain.RealtimeStockIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RealtimeStockIndexRepositoryImpl implements RealtimeStockIndexRepository {

    @Override
    public void addRealtimeStockIndex(RealtimeStockIndex realtimeStockIndex) {

    }

    @Override
    public void renewRealtimeStockIndex(RealtimeStockIndex realtimeStockIndex) {

    }

    @Override
    public void removeRealtimeStockIndex(RealtimeStockIndex realtimeStockIndex) {

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