package depth.finvibe.investment.modules.market.application.port.out;

import java.time.Duration;

public interface LeadershipLock {

    boolean tryAcquire(String lockKey, Duration ttl);

    boolean renew(String lockKey, Duration ttl);

    void release(String lockKey);
}
