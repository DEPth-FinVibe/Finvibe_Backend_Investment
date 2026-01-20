package depth.finvibe.investment.modules.market.infra.websocket.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MarketWebSocketRegistry {
    private final Map<String, MarketWebSocketConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> topicSubscribers = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> userSubscriptions = new ConcurrentHashMap<>();

    public MarketWebSocketConnection register(WebSocketSession session) {
        MarketWebSocketConnection connection = new MarketWebSocketConnection(session);
        connections.put(session.getId(), connection);
        return connection;
    }

    public MarketWebSocketConnection getConnection(String sessionId) {
        return connections.get(sessionId);
    }

    public void remove(String sessionId) {
        MarketWebSocketConnection connection = connections.remove(sessionId);
        if (connection == null) {
            return;
        }
        depth.finvibe.investment.modules.market.infra.websocket.WebSocketSession state = connection.getState();
        UUID userId = connection.getUserId();
        if (userId != null) {
            Map<String, Integer> userTopics = userSubscriptions.get(userId);
            if (userTopics != null) {
                for (String topic : state.getSubscribedTopics()) {
                    decrementUserTopic(userTopics, topic);
                }
                if (userTopics.isEmpty()) {
                    userSubscriptions.remove(userId);
                }
            }
        }
        for (String topic : state.getSubscribedTopics()) {
            Set<String> subscribers = topicSubscribers.get(topic);
            if (subscribers != null) {
                subscribers.remove(sessionId);
                if (subscribers.isEmpty()) {
                    topicSubscribers.remove(topic);
                }
            }
        }
    }

    public void authenticate(MarketWebSocketConnection connection, UUID userId) {
        connection.getState().authenticate(userId.toString());
        userSubscriptions.computeIfAbsent(userId, key -> new ConcurrentHashMap<>());
    }

    public SubscribeResult subscribe(MarketWebSocketConnection connection, List<String> topics, int limit) {
        depth.finvibe.investment.modules.market.infra.websocket.WebSocketSession state = connection.getState();
        UUID userId = connection.getUserId();
        if (userId == null) {
            return SubscribeResult.unauthorized();
        }

        Map<String, Integer> userTopics = userSubscriptions.computeIfAbsent(userId, key -> new ConcurrentHashMap<>());
        int uniqueTopicCount = userTopics.size();
        List<String> subscribed = new ArrayList<>();
        List<String> alreadySubscribed = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        boolean limitExceeded = false;

        for (String topic : topics) {
            if (state.getSubscribedTopics().contains(topic)) {
                alreadySubscribed.add(topic);
                continue;
            }
            if (!userTopics.containsKey(topic) && uniqueTopicCount >= limit) {
                rejected.add(topic);
                limitExceeded = true;
                continue;
            }
            state.getSubscribedTopics().add(topic);
            boolean isNewTopic = !userTopics.containsKey(topic);
            userTopics.merge(topic, 1, Integer::sum);
            if (isNewTopic) {
                uniqueTopicCount++;
            }
            topicSubscribers.computeIfAbsent(topic, key -> ConcurrentHashMap.newKeySet()).add(connection.getSession().getId());
            subscribed.add(topic);
        }

        return new SubscribeResult(subscribed, alreadySubscribed, rejected, limitExceeded);
    }

    public UnsubscribeResult unsubscribe(MarketWebSocketConnection connection, List<String> topics) {
        depth.finvibe.investment.modules.market.infra.websocket.WebSocketSession state = connection.getState();
        UUID userId = connection.getUserId();
        List<String> unsubscribed = new ArrayList<>();
        List<String> notSubscribed = new ArrayList<>();

        for (String topic : topics) {
            if (!state.getSubscribedTopics().contains(topic)) {
                notSubscribed.add(topic);
                continue;
            }
            state.getSubscribedTopics().remove(topic);
            unsubscribed.add(topic);

            Set<String> subscribers = topicSubscribers.get(topic);
            if (subscribers != null) {
                subscribers.remove(connection.getSession().getId());
                if (subscribers.isEmpty()) {
                    topicSubscribers.remove(topic);
                }
            }
            if (userId != null) {
                Map<String, Integer> userTopics = userSubscriptions.get(userId);
                if (userTopics != null) {
                    decrementUserTopic(userTopics, topic);
                    if (userTopics.isEmpty()) {
                        userSubscriptions.remove(userId);
                    }
                }
            }
        }

        return new UnsubscribeResult(unsubscribed, notSubscribed);
    }

    public List<MarketWebSocketConnection> getSubscribers(String topic) {
        Set<String> subscriberIds = topicSubscribers.get(topic);
        if (subscriberIds == null || subscriberIds.isEmpty()) {
            return List.of();
        }
        List<MarketWebSocketConnection> result = new ArrayList<>();
        for (String sessionId : subscriberIds) {
            MarketWebSocketConnection connection = connections.get(sessionId);
            if (connection != null) {
                result.add(connection);
            }
        }
        return result;
    }

    public record SubscribeResult(
            List<String> subscribed,
            List<String> alreadySubscribed,
            List<String> rejected,
            boolean limitExceeded
    ) {
        static SubscribeResult unauthorized() {
            return new SubscribeResult(List.of(), List.of(), List.of(), false);
        }
    }

    public record UnsubscribeResult(
            List<String> unsubscribed,
            List<String> notSubscribed
    ) {}

    private void decrementUserTopic(Map<String, Integer> userTopics, String topic) {
        Integer count = userTopics.get(topic);
        if (count == null) {
            return;
        }
        if (count <= 1) {
            userTopics.remove(topic);
        } else {
            userTopics.put(topic, count - 1);
        }
    }
}
