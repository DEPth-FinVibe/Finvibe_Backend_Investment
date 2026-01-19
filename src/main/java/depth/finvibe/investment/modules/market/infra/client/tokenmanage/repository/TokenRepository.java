package depth.finvibe.investment.modules.market.infra.client.tokenmanage.repository;

import java.time.LocalDateTime;

public interface TokenRepository {
    String getAccessToken();

    LocalDateTime getExpiresAt();

    void saveToken(String token, LocalDateTime expiresAt);

    boolean acquireRefreshLock();

    void releaseRefreshLock();
}
