package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.modules.market.dto.SubscriptionDto;

import java.util.List;
import java.util.Set;

public interface WebSocketUseCase {
    void createSession(String sessionId);
    void authenticateSession(String sessionId, String userId);
    boolean isAuthenticated(String sessionId);
    SubscriptionDto.Result subscribe(String sessionId, List<String> topics);
    SubscriptionDto.UnsubscribeResult unsubscribe(String sessionId, List<String> topics);
    void closeSession(String sessionId);
    Set<String> getSubscribers(String topic);
    void updatePingTime(String sessionId);
    void incrementMissedPong(String sessionId);
    boolean shouldDisconnect(String sessionId);
}
