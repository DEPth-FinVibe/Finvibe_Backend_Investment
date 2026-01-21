package depth.finvibe.investment.modules.market.infra.client.tokenmanage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import depth.finvibe.investment.modules.market.infra.client.tokenmanage.repository.TokenRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class KisTokenManager {

    private final KisTokenClient tokenClient;
    private final TokenRepository tokenRepository;
    private volatile String accessToken;

    private static final ZoneId KIS_ZONE = ZoneId.of("Asia/Seoul");

    public String getAccessToken() {
        if (accessToken != null) {
            return accessToken;
        }
        CachedToken cachedToken = readTokenFromRepository();
        if (cachedToken != null && !cachedToken.isExpiringSoon()) {
            this.accessToken = cachedToken.token();
            return accessToken;
        }
        refreshToken();
        return accessToken;
    }

    public LocalDateTime initAndGetNextRefreshTime() {
        CachedToken cachedToken = readTokenFromRepository();
        if (cachedToken == null || cachedToken.isExpiringSoon()) {
            return refreshTokenAndGetNextRefreshTime();
        }
        this.accessToken = cachedToken.token();
        return calculateNextRefreshTime(cachedToken.expiresAt());
    }

    public LocalDateTime refreshTokenAndGetNextRefreshTime() {
        CachedToken refreshed = refreshToken();
        if (refreshed == null) {
            return null;
        }
        return calculateNextRefreshTime(refreshed.expiresAt());
    }

    public CachedToken refreshToken() {
        if (!tokenRepository.acquireRefreshLock()) {
            waitForSharedToken();
            return readTokenFromRepository();
        }
        try {
            KisTokenClient.TokenResponse response = tokenClient.requestAccessToken();
            if (response == null) {
                return null;
            }
            LocalDateTime expiresAt = LocalDateTime.now(KIS_ZONE).plusSeconds(response.expiresIn());
            tokenRepository.saveToken(response.accessToken(), expiresAt);
            this.accessToken = response.accessToken();
            return new CachedToken(response.accessToken(), expiresAt);
        } finally {
            tokenRepository.releaseRefreshLock();
        }
    }

    private LocalDateTime calculateNextRefreshTime(LocalDateTime expiresAt) {
        LocalDateTime sixHoursLater = LocalDateTime.now(KIS_ZONE).plusHours(6);
        LocalDateTime safeRefreshTime = expiresAt.minusMinutes(10);
        return safeRefreshTime.isBefore(sixHoursLater) ? safeRefreshTime : sixHoursLater;
    }

    private CachedToken readTokenFromRepository() {
        String token = tokenRepository.getAccessToken();
        LocalDateTime expiresAt = tokenRepository.getExpiresAt();
        if (token == null || expiresAt == null) {
            return null;
        }
        return new CachedToken(token, expiresAt);
    }

    private void waitForSharedToken() {
        int attempts = 10;
        while (attempts-- > 0) {
            CachedToken cachedToken = readTokenFromRepository();
            if (cachedToken != null && !cachedToken.isExpiringSoon()) {
                this.accessToken = cachedToken.token();
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public record CachedToken(String token, LocalDateTime expiresAt) {
        boolean isExpiringSoon() {
            return expiresAt.isBefore(LocalDateTime.now(KIS_ZONE).plusMinutes(10));
        }
    }
}
