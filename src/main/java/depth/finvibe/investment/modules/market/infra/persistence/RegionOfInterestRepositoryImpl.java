package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.RegionOfInterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RegionOfInterestRepositoryImpl implements RegionOfInterestRepository {

    private static final String ROI_LEVEL1_SET = "roi:level1:stocks";
    private static final String ROI_LEVEL2_SET = "roi:level2:stocks";
    private static final String ROI_LEVEL1_COUNT_PREFIX = "roi:level1:count:";
    private static final String ROI_LEVEL2_COUNT_PREFIX = "roi:level2:count:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void addToLevel1(Long stockId) {
        String stockIdStr = String.valueOf(stockId);
        String countKey = ROI_LEVEL1_COUNT_PREFIX + stockIdStr;

        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count == 1) {
            redisTemplate.opsForSet().add(ROI_LEVEL1_SET, stockIdStr);
            log.debug("Added stock {} to Level 1 ROI", stockId);
        }
    }

    @Override
    public void addToLevel2(Long stockId) {
        String stockIdStr = String.valueOf(stockId);
        String countKey = ROI_LEVEL2_COUNT_PREFIX + stockIdStr;

        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count == 1) {
            redisTemplate.opsForSet().add(ROI_LEVEL2_SET, stockIdStr);
            log.debug("Added stock {} to Level 2 ROI", stockId);
        }
    }

    @Override
    public void removeFromLevel1(Long stockId) {
        String stockIdStr = String.valueOf(stockId);
        String countKey = ROI_LEVEL1_COUNT_PREFIX + stockIdStr;

        Long count = redisTemplate.opsForValue().decrement(countKey);
        if (count != null && count <= 0) {
            redisTemplate.opsForSet().remove(ROI_LEVEL1_SET, stockIdStr);
            redisTemplate.delete(countKey);
            log.debug("Removed stock {} from Level 1 ROI", stockId);
        }
    }

    @Override
    public void removeFromLevel2(Long stockId) {
        String stockIdStr = String.valueOf(stockId);
        String countKey = ROI_LEVEL2_COUNT_PREFIX + stockIdStr;

        Long count = redisTemplate.opsForValue().decrement(countKey);
        if (count != null && count <= 0) {
            redisTemplate.opsForSet().remove(ROI_LEVEL2_SET, stockIdStr);
            redisTemplate.delete(countKey);
            log.debug("Removed stock {} from Level 2 ROI", stockId);
        }
    }

    @Override
    public Set<Long> getLevel1StockIds() {
        Set<String> members = redisTemplate.opsForSet().members(ROI_LEVEL1_SET);
        return members != null ? members.stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet()) : Set.of();
    }

    @Override
    public Set<Long> getLevel2StockIds() {
        Set<String> members = redisTemplate.opsForSet().members(ROI_LEVEL2_SET);
        return members != null ? members.stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet()) : Set.of();
    }

    @Override
    public Long getLevel1Count(Long stockId) {
        String countKey = ROI_LEVEL1_COUNT_PREFIX + stockId;
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    @Override
    public Long getLevel2Count(Long stockId) {
        String countKey = ROI_LEVEL2_COUNT_PREFIX + stockId;
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0L;
    }

    @Override
    public boolean existsInLevel1(Long stockId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet()
                .isMember(ROI_LEVEL1_SET, String.valueOf(stockId)));
    }

    @Override
    public boolean existsInLevel2(Long stockId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet()
                .isMember(ROI_LEVEL2_SET, String.valueOf(stockId)));
    }
}