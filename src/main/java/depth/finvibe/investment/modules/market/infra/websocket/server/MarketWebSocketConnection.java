package depth.finvibe.investment.modules.market.infra.websocket.server;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import org.springframework.web.socket.WebSocketSession;

public class MarketWebSocketConnection {
    private final WebSocketSession session;
    private final depth.finvibe.investment.modules.market.infra.websocket.WebSocketSession state;
    private ScheduledFuture<?> authTimeoutTask;
    private ScheduledFuture<?> heartbeatTask;
    private long rateWindowSecond = -1;
    private int rateCount = 0;

    public MarketWebSocketConnection(WebSocketSession session) {
        this.session = session;
        this.state = new depth.finvibe.investment.modules.market.infra.websocket.WebSocketSession(session.getId());
    }

    public WebSocketSession getSession() {
        return session;
    }

    public depth.finvibe.investment.modules.market.infra.websocket.WebSocketSession getState() {
        return state;
    }

    public ScheduledFuture<?> getAuthTimeoutTask() {
        return authTimeoutTask;
    }

    public void setAuthTimeoutTask(ScheduledFuture<?> authTimeoutTask) {
        this.authTimeoutTask = authTimeoutTask;
    }

    public ScheduledFuture<?> getHeartbeatTask() {
        return heartbeatTask;
    }

    public void setHeartbeatTask(ScheduledFuture<?> heartbeatTask) {
        this.heartbeatTask = heartbeatTask;
    }

    public boolean tryConsume(int limitPerSecond) {
        long nowSecond = Instant.now().getEpochSecond();
        if (rateWindowSecond != nowSecond) {
            rateWindowSecond = nowSecond;
            rateCount = 0;
        }
        if (rateCount >= limitPerSecond) {
            return false;
        }
        rateCount++;
        return true;
    }

    public UUID getUserId() {
        if (state.getUserId() == null) {
            return null;
        }
        return UUID.fromString(state.getUserId());
    }
}
