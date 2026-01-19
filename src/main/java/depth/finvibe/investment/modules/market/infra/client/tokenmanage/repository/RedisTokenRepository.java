package depth.finvibe.investment.modules.market.infra.client.tokenmanage.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Repository
public class RedisTokenRepository implements TokenRepository {
    private static final ZoneId KIS_ZONE = ZoneId.of("Asia/Seoul");
    private static final String TOKEN_KEY = "kis:accessToken";
    private static final String EXPIRES_AT_KEY = "kis:accessToken:expiresAt";
    private static final String REFRESH_LOCK_KEY = "kis:accessToken:refreshLock";
    private static final Duration REFRESH_LOCK_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    public RedisTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getAccessToken() {
        return redisTemplate.opsForValue().get(TOKEN_KEY);
    }

    @Override
    public LocalDateTime getExpiresAt() {
        String expiresAtRaw = redisTemplate.opsForValue().get(EXPIRES_AT_KEY);
        if (expiresAtRaw == null) {
            return null;
        }
        try {
            long epochSeconds = Long.parseLong(expiresAtRaw);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), KIS_ZONE);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void saveToken(String token, LocalDateTime expiresAt) {
        long ttlSeconds = Math.max(0, Duration.between(LocalDateTime.now(KIS_ZONE), expiresAt).getSeconds());
        redisTemplate.opsForValue().set(TOKEN_KEY, token, Duration.ofSeconds(ttlSeconds));
        redisTemplate.opsForValue().set(
                EXPIRES_AT_KEY,
                String.valueOf(expiresAt.atZone(KIS_ZONE).toEpochSecond()),
                Duration.ofSeconds(ttlSeconds)
        );
    }

    @Override
    public boolean acquireRefreshLock() {
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(REFRESH_LOCK_KEY, lockValue, REFRESH_LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseRefreshLock() {
        redisTemplate.delete(REFRESH_LOCK_KEY);
    }
}
