package depth.finvibe.investment.modules.market.infra.redis;

import depth.finvibe.investment.modules.market.application.port.out.RealtimeStockIndexRepository;
import depth.finvibe.investment.modules.market.domain.RealtimeStockIndex;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RealtimeStockIndexRepositoryImpl implements RealtimeStockIndexRepository {

    private static final String KEY_PREFIX = "market:realtime-index:";
    private static final Duration INDEX_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addRealtimeStockIndex(RealtimeStockIndex realtimeStockIndex) {
        String key = keyForStock(realtimeStockIndex.getStockId());
        redisTemplate.opsForSet().add(key, realtimeStockIndex.getWatcherId().toString());
        redisTemplate.expire(key, INDEX_TTL);
    }

    @Override
    public void renewRealtimeStockIndex(RealtimeStockIndex realtimeStockIndex) {
        String key = keyForStock(realtimeStockIndex.getStockId());
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, INDEX_TTL);
        } else {
            addRealtimeStockIndex(realtimeStockIndex);
        }
    }

    @Override
    public void removeRealtimeStockIndex(RealtimeStockIndex realtimeStockIndex) {
        String key = keyForStock(realtimeStockIndex.getStockId());
        redisTemplate.opsForSet().remove(key, realtimeStockIndex.getWatcherId().toString());
        Long remaining = redisTemplate.opsForSet().size(key);
        if (remaining != null && remaining == 0L) {
            redisTemplate.delete(key);
        }
    }

    @Override
    public boolean existsByStockId(Long stockId) {
        String key = keyForStock(stockId);
        Long size = redisTemplate.opsForSet().size(key);
        return size != null && size > 0;
    }

    @Override
    public boolean allExistsByStockIds(Iterable<Long> stockIds) {
        for (Long stockId : stockIds) {
            if (!existsByStockId(stockId)) {
                return false;
            }
        }
        return true;
    }

    private String keyForStock(Long stockId) {
        return KEY_PREFIX + stockId;
    }
}
