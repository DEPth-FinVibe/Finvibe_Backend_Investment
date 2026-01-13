package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.domain.error.WebSocketErrorCode;
import depth.finvibe.investment.modules.market.dto.SubscriptionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MarketSubscriptionServiceTest {

    @InjectMocks
    private MarketSubscriptionService subscriptionService;

    private static final String SESSION_ID = "session123";
    private static final String USER_ID = "user123";
    private static final String TOPIC_STOCK_1 = "quote:1";
    private static final String TOPIC_STOCK_2 = "quote:2";
    private static final String INVALID_TOPIC = "price.AAPL";

    @BeforeEach
    void setUp() {
        subscriptionService = new MarketSubscriptionService();
    }

    @Test
    @DisplayName("세션 생성 - 성공")
    void createSession_Success() {
        // when
        subscriptionService.createSession(SESSION_ID);

        // then
        assertThat(subscriptionService.isAuthenticated(SESSION_ID)).isFalse();
    }

    @Test
    @DisplayName("세션 인증 - 성공")
    void authenticateSession_Success() {
        // given
        subscriptionService.createSession(SESSION_ID);

        // when
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);

        // then
        assertThat(subscriptionService.isAuthenticated(SESSION_ID)).isTrue();
    }

    @Test
    @DisplayName("세션 인증 - 존재하지 않는 세션")
    void authenticateSession_SessionNotFound() {
        // when
        subscriptionService.authenticateSession("invalid", USER_ID);

        // then - 예외를 던지지 않고 조용히 무시
        assertThat(subscriptionService.isAuthenticated("invalid")).isFalse();
    }

    @Test
    @DisplayName("구독 - 성공")
    void subscribe_Success() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        List<String> topics = Arrays.asList(TOPIC_STOCK_1, TOPIC_STOCK_2);

        // when
        SubscriptionDto.Result result = subscriptionService.subscribe(SESSION_ID, topics);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubscribed()).containsExactlyInAnyOrder(TOPIC_STOCK_1, TOPIC_STOCK_2);
        assertThat(result.getAlreadySubscribed()).isEmpty();
        assertThat(result.getRejected()).isEmpty();
        assertThat(subscriptionService.getSubscribers(TOPIC_STOCK_1)).contains(SESSION_ID);
        assertThat(subscriptionService.getSubscribers(TOPIC_STOCK_2)).contains(SESSION_ID);
    }

    @Test
    @DisplayName("구독 - 미인증 세션")
    void subscribe_NotAuthenticated() {
        // given
        subscriptionService.createSession(SESSION_ID);
        List<String> topics = Arrays.asList(TOPIC_STOCK_1);

        // when
        SubscriptionDto.Result result = subscriptionService.subscribe(SESSION_ID, topics);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(WebSocketErrorCode.UNAUTHORIZED);
        assertThat(result.getSubscribed()).isEmpty();
    }

    @Test
    @DisplayName("구독 - 세션 없음")
    void subscribe_SessionNotFound() {
        // given
        List<String> topics = Arrays.asList(TOPIC_STOCK_1);

        // when
        SubscriptionDto.Result result = subscriptionService.subscribe("invalid", topics);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(WebSocketErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("구독 - 일부 중복 구독")
    void subscribe_PartiallyAlreadySubscribed() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        subscriptionService.subscribe(SESSION_ID, Arrays.asList(TOPIC_STOCK_1));

        // when
        List<String> topics = Arrays.asList(TOPIC_STOCK_1, TOPIC_STOCK_2);
        SubscriptionDto.Result result = subscriptionService.subscribe(SESSION_ID, topics);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubscribed()).containsExactly(TOPIC_STOCK_2);
        assertThat(result.getAlreadySubscribed()).containsExactly(TOPIC_STOCK_1);
        assertThat(result.getRejected()).isEmpty();
    }

    @Test
    @DisplayName("구독 취소 - 성공")
    void unsubscribe_Success() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        List<String> topics = Arrays.asList(TOPIC_STOCK_1, TOPIC_STOCK_2);
        subscriptionService.subscribe(SESSION_ID, topics);

        // when
        SubscriptionDto.UnsubscribeResult result = subscriptionService.unsubscribe(SESSION_ID, Arrays.asList(TOPIC_STOCK_1));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUnsubscribed()).containsExactly(TOPIC_STOCK_1);
        assertThat(result.getNotSubscribed()).isEmpty();
        assertThat(subscriptionService.getSubscribers(TOPIC_STOCK_1)).doesNotContain(SESSION_ID);
        assertThat(subscriptionService.getSubscribers(TOPIC_STOCK_2)).contains(SESSION_ID);
    }

    @Test
    @DisplayName("구독 취소 - 세션 없음")
    void unsubscribe_SessionNotFound() {
        // given
        List<String> topics = Arrays.asList(TOPIC_STOCK_1);

        // when
        SubscriptionDto.UnsubscribeResult result = subscriptionService.unsubscribe("invalid", topics);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(WebSocketErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("구독 취소 - 일부 구독하지 않은 토픽")
    void unsubscribe_PartiallyNotSubscribed() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        subscriptionService.subscribe(SESSION_ID, Arrays.asList(TOPIC_STOCK_1));

        // when
        List<String> topics = Arrays.asList(TOPIC_STOCK_1, TOPIC_STOCK_2);
        SubscriptionDto.UnsubscribeResult result = subscriptionService.unsubscribe(SESSION_ID, topics);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUnsubscribed()).containsExactly(TOPIC_STOCK_1);
        assertThat(result.getNotSubscribed()).containsExactly(TOPIC_STOCK_2);
    }

    @Test
    @DisplayName("세션 종료 - 성공")
    void closeSession_Success() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        List<String> topics = Arrays.asList(TOPIC_STOCK_1, TOPIC_STOCK_2);
        subscriptionService.subscribe(SESSION_ID, topics);

        // when
        subscriptionService.closeSession(SESSION_ID);

        // then
        assertThat(subscriptionService.getSubscribers(TOPIC_STOCK_1)).doesNotContain(SESSION_ID);
        assertThat(subscriptionService.getSubscribers(TOPIC_STOCK_2)).doesNotContain(SESSION_ID);
    }

    @Test
    @DisplayName("구독자 조회 - 여러 세션")
    void getSubscribers_MultipleSessions() {
        // given
        String session1 = "session1";
        String session2 = "session2";
        subscriptionService.createSession(session1);
        subscriptionService.createSession(session2);
        subscriptionService.authenticateSession(session1, "user1");
        subscriptionService.authenticateSession(session2, "user2");
        subscriptionService.subscribe(session1, Arrays.asList(TOPIC_STOCK_1));
        subscriptionService.subscribe(session2, Arrays.asList(TOPIC_STOCK_1));

        // when
        Set<String> subscribers = subscriptionService.getSubscribers(TOPIC_STOCK_1);

        // then
        assertThat(subscribers).hasSize(2);
        assertThat(subscribers).containsExactlyInAnyOrder(session1, session2);
    }

    @Test
    @DisplayName("구독자 조회 - 구독자 없음")
    void getSubscribers_NoSubscribers() {
        // when
        Set<String> subscribers = subscriptionService.getSubscribers(TOPIC_STOCK_1);

        // then
        assertThat(subscribers).isEmpty();
    }

    @Test
    @DisplayName("Ping 시간 업데이트")
    void updatePingTime() {
        // given
        subscriptionService.createSession(SESSION_ID);

        // when & then
        assertThatCode(() -> subscriptionService.updatePingTime(SESSION_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Pong 미응답 횟수 증가")
    void incrementMissedPong() {
        // given
        subscriptionService.createSession(SESSION_ID);

        // when & then
        assertThatCode(() -> subscriptionService.incrementMissedPong(SESSION_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("연결 종료 여부 - 미응답 횟수 초과")
    void shouldDisconnect_ExceededLimit() {
        // given
        subscriptionService.createSession(SESSION_ID);

        // when - 3회 이상
        for (int i = 0; i < 3; i++) {
            subscriptionService.incrementMissedPong(SESSION_ID);
        }

        // then
        assertThat(subscriptionService.shouldDisconnect(SESSION_ID)).isTrue();
    }

    @Test
    @DisplayName("연결 종료 여부 - 정상")
    void shouldDisconnect_Normal() {
        // given
        subscriptionService.createSession(SESSION_ID);

        // when & then
        assertThat(subscriptionService.shouldDisconnect(SESSION_ID)).isFalse();
    }

    @Test
    @DisplayName("연결 종료 여부 - 세션 없음")
    void shouldDisconnect_SessionNotFound() {
        // when & then
        assertThat(subscriptionService.shouldDisconnect("invalid")).isFalse();
    }

    @Test
    @DisplayName("구독 - 잘못된 토픽 형식")
    void subscribe_InvalidTopicFormat() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        List<String> topics = Arrays.asList(INVALID_TOPIC, TOPIC_STOCK_1);

        // when
        SubscriptionDto.Result result = subscriptionService.subscribe(SESSION_ID, topics);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubscribed()).containsExactly(TOPIC_STOCK_1);
        assertThat(result.getRejected()).containsExactly(INVALID_TOPIC);
        assertThat(result.getAlreadySubscribed()).isEmpty();
    }

    @Test
    @DisplayName("구독 - 제한 초과")
    void subscribe_LimitExceeded() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);

        // 30개 구독
        List<String> topics = new java.util.ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            topics.add("quote:" + i);
        }
        subscriptionService.subscribe(SESSION_ID, topics);

        // when - 31번째 구독 시도
        SubscriptionDto.Result result = subscriptionService.subscribe(SESSION_ID, Arrays.asList("quote:31"));

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(WebSocketErrorCode.SUBSCRIPTION_LIMIT_EXCEEDED);
    }
}