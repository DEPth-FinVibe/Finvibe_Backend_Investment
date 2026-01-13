package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.WebSocketUseCase;
import depth.finvibe.investment.modules.market.domain.error.WebSocketErrorCode;
import depth.finvibe.investment.modules.market.dto.SubscriptionDto;
import depth.finvibe.investment.modules.market.dto.WebSocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketSubscriptionService implements WebSocketUseCase {

    private static final int MAX_SUBSCRIPTIONS_PER_SESSION = 30;

    // sessionId -> WebSocketSession
    private final Map<String, depth.finvibe.investment.modules.market.dto.WebSocketSession> sessions = new ConcurrentHashMap<>();

    // topic -> Set<sessionId> 매핑 (역방향 조회용)
    private final Map<String, Set<String>> topicSubscribers = new ConcurrentHashMap<>();


    public void createSession(String sessionId) {
        sessions.putIfAbsent(sessionId, new WebSocketSession(sessionId));
        log.info("WebSocket session created: {}", sessionId);
    }

    public void authenticateSession(String sessionId, String userId) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.authenticate(userId);
            log.info("Session {} authenticated for user {}", sessionId, userId);
        }
    }

    public boolean isAuthenticated(String sessionId) {
        WebSocketSession session = sessions.get(sessionId);
        return session != null && session.isAuthenticated() && !session.isAuthenticationExpired(30);
    }

    public SubscriptionDto.Result subscribe(String sessionId, List<String> topics) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null) {
            return SubscriptionDto.Result.error(WebSocketErrorCode.UNAUTHORIZED);
        }

        if (!session.isAuthenticated()) {
            return SubscriptionDto.Result.error(WebSocketErrorCode.UNAUTHORIZED);
        }

        List<String> subscribed = new ArrayList<>();
        List<String> alreadySubscribed = new ArrayList<>();
        List<String> rejected = new ArrayList<>();

        for (String topic : topics) {
            // 토픽 포맷 검증 (quote:{stockId})
            if (!isValidTopic(topic)) {
                rejected.add(topic);
                continue;
            }

            // 이미 구독 중인 토픽
            if (session.getSubscribedTopics().contains(topic)) {
                alreadySubscribed.add(topic);
                continue;
            }

            // 구독 제한 체크
            if (session.getSubscribedTopics().size() >= MAX_SUBSCRIPTIONS_PER_SESSION) {
                return SubscriptionDto.Result.error(WebSocketErrorCode.SUBSCRIPTION_LIMIT_EXCEEDED);
            }

            // 구독 추가
            session.getSubscribedTopics().add(topic);
            topicSubscribers
                    .computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet())
                    .add(sessionId);
            subscribed.add(topic);
        }

        log.info("Session {} subscribed to topics: {}", sessionId, subscribed);
        return SubscriptionDto.Result.success(subscribed, alreadySubscribed, rejected);
    }

    public SubscriptionDto.UnsubscribeResult unsubscribe(String sessionId, List<String> topics) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null) {
            return SubscriptionDto.UnsubscribeResult.error(WebSocketErrorCode.UNAUTHORIZED);
        }

        List<String> unsubscribed = new ArrayList<>();
        List<String> notSubscribed = new ArrayList<>();

        for (String topic : topics) {
            if (session.getSubscribedTopics().remove(topic)) {
                Set<String> subscribers = topicSubscribers.get(topic);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                }
                unsubscribed.add(topic);
            } else {
                notSubscribed.add(topic);
            }
        }

        log.info("Session {} unsubscribed from topics: {}", sessionId, unsubscribed);
        return SubscriptionDto.UnsubscribeResult.success(unsubscribed, notSubscribed);
    }

    public void closeSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null) {
            session.getSubscribedTopics().forEach(topic -> {
                Set<String> subscribers = topicSubscribers.get(topic);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                }
            });
            log.info("Session {} closed and all subscriptions removed", sessionId);
        }
    }

    public Set<String> getSubscribers(String topic) {
        return topicSubscribers.getOrDefault(topic, Collections.emptySet());
    }

    public void updatePingTime(String sessionId) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.resetPongCount();
        }
    }

    public void incrementMissedPong(String sessionId) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.incrementMissedPong();
        }
    }

    public boolean shouldDisconnect(String sessionId) {
        WebSocketSession session = sessions.get(sessionId);
        return session != null && session.shouldDisconnect();
    }

    private boolean isValidTopic(String topic) {
        if (topic == null || !topic.startsWith("quote:")) {
            return false;
        }
        try {
            Long.parseLong(topic.substring(6));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}