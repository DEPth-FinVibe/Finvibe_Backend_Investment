package depth.finvibe.investment.modules.market.application;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;

@Slf4j
@Service
public class RegionOfInterestService {

    private static final String ROI_LEVEL1_SET = "roi:level1:stocks";
    private static final String ROI_LEVEL2_SET = "roi:level2:stocks";
    private static final String ROI_LEVEL1_COUNT_PREFIX = "roi:level1:count:";
    private static final String ROI_LEVEL2_COUNT_PREFIX = "roi:level2:count:";

    private final RedisTemplate<String, String> redisTemplate;

    public RegionOfInterestService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addToLevel1(Set<Long> stockIds) {
        for (Long stockId : stockIds) {
            String stockIdStr = String.valueOf(stockId);
            String countKey = ROI_LEVEL1_COUNT_PREFIX + stockIdStr;

            Long count = redisTemplate.opsForValue().increment(countKey);
            if (count == 1) {
                redisTemplate.opsForSet().add(ROI_LEVEL1_SET, stockIdStr);
                log.debug("Added stock {} to Level 1 ROI", stockId);
            }
        }
    }

    public void addToLevel2(Set<Long> stockIds) {
        for (Long stockId : stockIds) {
            String stockIdStr = String.valueOf(stockId);
            String countKey = ROI_LEVEL2_COUNT_PREFIX + stockIdStr;

            Long count = redisTemplate.opsForValue().increment(countKey);
            if (count == 1) {
                redisTemplate.opsForSet().add(ROI_LEVEL2_SET, stockIdStr);
                log.debug("Added stock {} to Level 2 ROI", stockId);
            }
        }
    }

    public void removeFromLevel1(Set<Long> stockIds) {
        for (Long stockId : stockIds) {
            String stockIdStr = String.valueOf(stockId);
            String countKey = ROI_LEVEL1_COUNT_PREFIX + stockIdStr;

            Long count = redisTemplate.opsForValue().decrement(countKey);
            if (count != null && count <= 0) {
                redisTemplate.opsForSet().remove(ROI_LEVEL1_SET, stockIdStr);
                redisTemplate.delete(countKey);
                log.debug("Removed stock {} from Level 1 ROI", stockId);
            }
        }
    }

    public void removeFromLevel2(Set<Long> stockIds) {
        for (Long stockId : stockIds) {
            String stockIdStr = String.valueOf(stockId);
            String countKey = ROI_LEVEL2_COUNT_PREFIX + stockIdStr;

            Long count = redisTemplate.opsForValue().decrement(countKey);
            if (count != null && count <= 0) {
                redisTemplate.opsForSet().remove(ROI_LEVEL2_SET, stockIdStr);
                redisTemplate.delete(countKey);
                log.debug("Removed stock {} from Level 2 ROI", stockId);
            }
        }
    }

    public Set<String> getLevel1StockIds() {
        return redisTemplate.opsForSet().members(ROI_LEVEL1_SET);
    }

    public Set<String> getLevel2StockIds() {
        return redisTemplate.opsForSet().members(ROI_LEVEL2_SET);
    }

    public Long getLevel1Count(Long stockId) {
        String countKey = ROI_LEVEL1_COUNT_PREFIX + stockId;
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    public Long getLevel2Count(Long stockId) {
        String countKey = ROI_LEVEL2_COUNT_PREFIX + stockId;
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }
}