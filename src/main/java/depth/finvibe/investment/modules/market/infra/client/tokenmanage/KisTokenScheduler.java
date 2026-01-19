package depth.finvibe.investment.modules.market.infra.client.tokenmanage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class KisTokenScheduler {

    private final KisTokenManager tokenManager;
    private final TaskScheduler taskScheduler;

    private static final ZoneId KIS_ZONE = ZoneId.of("Asia/Seoul");
    @PostConstruct
    public void init() {
        LocalDateTime nextRefreshTime = tokenManager.initAndGetNextRefreshTime();
        scheduleNext(nextRefreshTime);
    }

    private void scheduleNext(LocalDateTime nextRefreshTime) {
        if (nextRefreshTime == null) {
            taskScheduler.schedule(this::refreshAndScheduleNext, LocalDateTime.now(KIS_ZONE).plusMinutes(1).atZone(KIS_ZONE).toInstant());
            return;
        }
        taskScheduler.schedule(this::refreshAndScheduleNext, nextRefreshTime.atZone(KIS_ZONE).toInstant());
    }

    private void refreshAndScheduleNext() {
        LocalDateTime nextRefreshTime = tokenManager.refreshTokenAndGetNextRefreshTime();
        scheduleNext(nextRefreshTime);
    }
}
