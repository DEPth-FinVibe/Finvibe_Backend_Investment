package depth.finvibe.investment.modules.market.infra.client;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KisRateLimiter {

    private static final long SECOND_WINDOW_MILLIS = 1000L;
    private static final long MINUTE_WINDOW_MILLIS = 60_000L;

    private static final RedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end " +
            "local ttl = redis.call('PTTL', KEYS[1]) " +
            "return {current, ttl}",
            List.class
    );

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final long secondLimit;
    private final long minuteLimit;

    public KisRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${market.kis.rate-limit.key-prefix:kis:rate}") String keyPrefix,
            @Value("${market.kis.rate-limit.second:20}") long secondLimit,
            @Value("${market.kis.rate-limit.minute:15}") long minuteLimit
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.secondLimit = secondLimit;
        this.minuteLimit = minuteLimit;
    }

    public void acquire(String key) {
        String secondKey = keyPrefix + ":second:" + key;
        String minuteKey = keyPrefix + ":minute:" + key;

        while (true) {
            RateResult secondResult = increment(secondKey, SECOND_WINDOW_MILLIS);
            RateResult minuteResult = increment(minuteKey, MINUTE_WINDOW_MILLIS);

            long waitMillis = 0L;
            if (secondResult.count > secondLimit) {
                waitMillis = Math.max(waitMillis, secondResult.ttlMillis);
            }
            if (minuteResult.count > minuteLimit) {
                waitMillis = Math.max(waitMillis, minuteResult.ttlMillis);
            }

            if (waitMillis <= 0L) {
                return;
            }

            sleep(waitMillis, key);
        }
    }

    private RateResult increment(String key, long windowMillis) {
        List<Long> result = (List<Long>) redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(windowMillis)
        );

        if (result == null || result.size() < 2) {
            throw new IllegalStateException("KIS rate limiter returned empty result.");
        }

        long count = result.get(0) == null ? 0L : result.get(0);
        long ttl = result.get(1) == null ? 0L : result.get(1);
        if (ttl < 0L) {
            ttl = windowMillis;
        }

        return new RateResult(count, ttl);
    }

    private void sleep(long waitMillis, String key) {
        if (waitMillis <= 0L) {
            return;
        }
        try {
            log.debug("KIS rate limit waiting {}ms for key={}", waitMillis, key);
            Thread.sleep(waitMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("KIS rate limit wait interrupted.", ex);
        }
    }

    private record RateResult(long count, long ttlMillis) {
    }
}
