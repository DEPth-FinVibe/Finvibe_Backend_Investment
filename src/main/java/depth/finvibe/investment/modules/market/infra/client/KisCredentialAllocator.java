package depth.finvibe.investment.modules.market.infra.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.market.infra.config.KisCredentialsProperties;
import depth.finvibe.investment.modules.market.infra.config.KisCredentialsProperties.Credential;
import depth.finvibe.investment.modules.market.infra.lock.ActiveNodeRegistry;

@Slf4j
@Component
public class KisCredentialAllocator {
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(60);
    private static final Duration DEFAULT_RENEW_INTERVAL = Duration.ofSeconds(20);
    private static final int DEFAULT_RETRY_MAX = 50;
    private static final long DEFAULT_RETRY_DELAY_MILLIS = 200L;
    private static final String DEFAULT_KEY_PREFIX = "kis:credential:lock";

    private static final RedisScript<Long> RENEW_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('pexpire', KEYS[1], ARGV[2]) " +
            "else return 0 end",
            Long.class
    );

    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end",
            Long.class
    );

    private final KisCredentialsProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final TaskScheduler taskScheduler;
    private final ActiveNodeRegistry activeNodeRegistry;
    private final String ownerId;
    private final AtomicInteger cursor = new AtomicInteger();

    private final Map<String, Credential> allocatedCredentials = new ConcurrentHashMap<>();

    public KisCredentialAllocator(
            KisCredentialsProperties properties,
            StringRedisTemplate redisTemplate,
            TaskScheduler taskScheduler,
            ActiveNodeRegistry activeNodeRegistry
    ) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.taskScheduler = taskScheduler;
        this.activeNodeRegistry = activeNodeRegistry;
        this.ownerId = UUID.randomUUID().toString();
    }

    @PostConstruct
    public void init() {
        rebalanceAndRenew();
        taskScheduler.scheduleAtFixedRate(this::rebalanceAndRenew, resolveRenewInterval());
    }

    public List<Credential> getAllocatedCredentials() {
        List<Credential> validCredentials = properties.getValidCredentials();
        if (validCredentials.isEmpty()) {
            return List.of();
        }

        List<Credential> ordered = new ArrayList<>();
        for (Credential credential : validCredentials) {
            if (allocatedCredentials.containsKey(credential.appKey())) {
                ordered.add(credential);
            }
        }
        return List.copyOf(ordered);
    }

    public Credential selectCredentialForRequest(KisRateLimiter rateLimiter) {
        List<Credential> candidates = getAllocatedCredentials();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("KIS credential is not allocated yet.");
        }

        int size = candidates.size();
        int start = Math.floorMod(cursor.getAndIncrement(), size);
        for (int i = 0; i < size; i++) {
            int index = (start + i) % size;
            Credential credential = candidates.get(index);
            if (rateLimiter.tryAcquire(credential.appKey())) {
                return credential;
            }
        }

        Credential fallback = candidates.get(start);
        rateLimiter.acquire(fallback.appKey());
        return fallback;
    }

    private void rebalanceAndRenew() {
        List<Credential> validCredentials = properties.getValidCredentials();
        if (validCredentials.isEmpty()) {
            throw new IllegalStateException("최소 하나의 유효한 KIS credential이 필요합니다");
        }

        renewAllocatedLocks();
        int targetCount = calculateTargetCount(validCredentials.size());
        adjustAllocation(validCredentials, targetCount);

        if (allocatedCredentials.isEmpty()) {
            throw new IllegalStateException("KIS credential allocation failed. No credentials assigned.");
        }
    }

    private void renewAllocatedLocks() {
        for (String appKey : List.copyOf(allocatedCredentials.keySet())) {
            if (!renewLock(appKey)) {
                allocatedCredentials.remove(appKey);
            }
        }
    }

    private void adjustAllocation(List<Credential> validCredentials, int targetCount) {
        int currentCount = allocatedCredentials.size();
        if (currentCount > targetCount) {
            releaseExtraCredentials(validCredentials, currentCount - targetCount);
        }
        if (allocatedCredentials.size() < targetCount) {
            acquireMissingCredentials(validCredentials, targetCount - allocatedCredentials.size());
        }
    }

    private void releaseExtraCredentials(List<Credential> validCredentials, int releaseCount) {
        List<Credential> allocated = new ArrayList<>();
        for (Credential credential : validCredentials) {
            if (allocatedCredentials.containsKey(credential.appKey())) {
                allocated.add(credential);
            }
        }

        for (int i = allocated.size() - 1; i >= 0 && releaseCount > 0; i--) {
            Credential credential = allocated.get(i);
            if (releaseLock(credential.appKey())) {
                allocatedCredentials.remove(credential.appKey());
                releaseCount--;
                log.info("Released KIS credential lock - appKey={}", credential.appKey());
            }
        }
    }

    private void acquireMissingCredentials(List<Credential> validCredentials, int acquireCount) {
        int attempts = 0;
        while (acquireCount > 0 && attempts++ < resolveRetryMax()) {
            for (Credential credential : validCredentials) {
                if (allocatedCredentials.containsKey(credential.appKey())) {
                    continue;
                }

                if (tryAcquire(credential)) {
                    acquireCount--;
                    if (acquireCount == 0) {
                        return;
                    }
                }
            }
            sleep(resolveRetryDelayMillis());
        }
    }

    private boolean tryAcquire(Credential credential) {
        String key = lockKey(credential.appKey());
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                key,
                ownerId,
                resolveTtl()
        );
        if (Boolean.TRUE.equals(acquired)) {
            allocatedCredentials.put(credential.appKey(), credential);
            log.info("Allocated KIS credential lock - appKey={}", credential.appKey());
            return true;
        }
        return false;
    }

    private boolean renewLock(String appKey) {
        Long result = redisTemplate.execute(
                RENEW_LOCK_SCRIPT,
                List.of(lockKey(appKey)),
                ownerId,
                String.valueOf(resolveTtl().toMillis())
        );
        return result != null && result == 1L;
    }

    private boolean releaseLock(String appKey) {
        Long result = redisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                List.of(lockKey(appKey)),
                ownerId
        );
        return result != null && result == 1L;
    }

    private int calculateTargetCount(int credentialCount) {
        int activeNodeCount = activeNodeRegistry.getActiveNodeCount();
        int target = (int) Math.ceil((double) credentialCount / activeNodeCount);
        return Math.min(target, credentialCount);
    }

    private Duration resolveTtl() {
        KisCredentialsProperties.CredentialLock lockConfig = properties.credentialLock();
        if (lockConfig == null || lockConfig.ttlSeconds() == null || lockConfig.ttlSeconds() <= 0) {
            return DEFAULT_TTL;
        }
        return Duration.ofSeconds(lockConfig.ttlSeconds());
    }

    private Duration resolveRenewInterval() {
        KisCredentialsProperties.CredentialLock lockConfig = properties.credentialLock();
        if (lockConfig == null || lockConfig.renewIntervalSeconds() == null || lockConfig.renewIntervalSeconds() <= 0) {
            return DEFAULT_RENEW_INTERVAL;
        }
        return Duration.ofSeconds(lockConfig.renewIntervalSeconds());
    }

    private int resolveRetryMax() {
        KisCredentialsProperties.CredentialLock lockConfig = properties.credentialLock();
        if (lockConfig == null || lockConfig.retryMax() == null || lockConfig.retryMax() <= 0) {
            return DEFAULT_RETRY_MAX;
        }
        return lockConfig.retryMax();
    }

    private long resolveRetryDelayMillis() {
        KisCredentialsProperties.CredentialLock lockConfig = properties.credentialLock();
        if (lockConfig == null || lockConfig.retryDelayMillis() == null || lockConfig.retryDelayMillis() <= 0) {
            return DEFAULT_RETRY_DELAY_MILLIS;
        }
        return lockConfig.retryDelayMillis();
    }

    private String lockKey(String appKey) {
        KisCredentialsProperties.CredentialLock lockConfig = properties.credentialLock();
        String prefix = DEFAULT_KEY_PREFIX;
        if (lockConfig != null && lockConfig.keyPrefix() != null && !lockConfig.keyPrefix().isBlank()) {
            prefix = lockConfig.keyPrefix();
        }
        return prefix + ":" + appKey;
    }

    private void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("KIS credential allocation interrupted.", ex);
        }
    }
}
