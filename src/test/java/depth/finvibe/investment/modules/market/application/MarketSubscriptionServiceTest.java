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
    private static final String TOPIC_AAPL = "price.AAPL";
    private static final String TOPIC_GOOGL = "price.GOOGL";

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
        // when & then
        assertThatThrownBy(() -> subscriptionService.authenticateSession("invalid", USER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("구독 - 성공")
    void subscribe_Success() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        List<String> topics = Arrays.asList(TOPIC_AAPL, TOPIC_GOOGL);

        // when
        SubscriptionDto.Result result = subscriptionService.subscribe(SESSION_ID, topics);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubscribed()).containsExactlyInAnyOrder(TOPIC_AAPL, TOPIC_GOOGL);
        assertThat(result.getAlreadySubscribed()).isEmpty();
        assertThat(result.getRejected()).isEmpty();
        assertThat(subscriptionService.getSubscribers(TOPIC_AAPL)).contains(SESSION_ID);
        assertThat(subscriptionService.getSubscribers(TOPIC_GOOGL)).contains(SESSION_ID);
    }

    @Test
    @DisplayName("구독 - 미인증 세션")
    void subscribe_NotAuthenticated() {
        // given
        subscriptionService.createSession(SESSION_ID);
        List<String> topics = Arrays.asList(TOPIC_AAPL);

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
        List<String> topics = Arrays.asList(TOPIC_AAPL);

        // when
        SubscriptionDto.Result result = subscriptionService.subscribe("invalid", topics);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(WebSocketErrorCode.WEBSOCKET_CONNECTION_FAILED);
    }

    @Test
    @DisplayName("구독 - 일부 중복 구독")
    void subscribe_PartiallyAlreadySubscribed() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        subscriptionService.subscribe(SESSION_ID, Arrays.asList(TOPIC_AAPL));

        // when
        List<String> topics = Arrays.asList(TOPIC_AAPL, TOPIC_GOOGL);
        SubscriptionDto.Result result = subscriptionService.subscribe(SESSION_ID, topics);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSubscribed()).containsExactly(TOPIC_GOOGL);
        assertThat(result.getAlreadySubscribed()).containsExactly(TOPIC_AAPL);
        assertThat(result.getRejected()).isEmpty();
    }

    @Test
    @DisplayName("구독 취소 - 성공")
    void unsubscribe_Success() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        List<String> topics = Arrays.asList(TOPIC_AAPL, TOPIC_GOOGL);
        subscriptionService.subscribe(SESSION_ID, topics);

        // when
        SubscriptionDto.UnsubscribeResult result = subscriptionService.unsubscribe(SESSION_ID, Arrays.asList(TOPIC_AAPL));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUnsubscribed()).containsExactly(TOPIC_AAPL);
        assertThat(result.getNotSubscribed()).isEmpty();
        assertThat(subscriptionService.getSubscribers(TOPIC_AAPL)).doesNotContain(SESSION_ID);
        assertThat(subscriptionService.getSubscribers(TOPIC_GOOGL)).contains(SESSION_ID);
    }

    @Test
    @DisplayName("구독 취소 - 세션 없음")
    void unsubscribe_SessionNotFound() {
        // given
        List<String> topics = Arrays.asList(TOPIC_AAPL);

        // when
        SubscriptionDto.UnsubscribeResult result = subscriptionService.unsubscribe("invalid", topics);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(WebSocketErrorCode.WEBSOCKET_CONNECTION_FAILED);
    }

    @Test
    @DisplayName("구독 취소 - 일부 구독하지 않은 토픽")
    void unsubscribe_PartiallyNotSubscribed() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        subscriptionService.subscribe(SESSION_ID, Arrays.asList(TOPIC_AAPL));

        // when
        List<String> topics = Arrays.asList(TOPIC_AAPL, TOPIC_GOOGL);
        SubscriptionDto.UnsubscribeResult result = subscriptionService.unsubscribe(SESSION_ID, topics);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUnsubscribed()).containsExactly(TOPIC_AAPL);
        assertThat(result.getNotSubscribed()).containsExactly(TOPIC_GOOGL);
    }

    @Test
    @DisplayName("세션 종료 - 성공")
    void closeSession_Success() {
        // given
        subscriptionService.createSession(SESSION_ID);
        subscriptionService.authenticateSession(SESSION_ID, USER_ID);
        List<String> topics = Arrays.asList(TOPIC_AAPL, TOPIC_GOOGL);
        subscriptionService.subscribe(SESSION_ID, topics);

        // when
        subscriptionService.closeSession(SESSION_ID);

        // then
        assertThat(subscriptionService.getSubscribers(TOPIC_AAPL)).doesNotContain(SESSION_ID);
        assertThat(subscriptionService.getSubscribers(TOPIC_GOOGL)).doesNotContain(SESSION_ID);
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
        subscriptionService.subscribe(session1, Arrays.asList(TOPIC_AAPL));
        subscriptionService.subscribe(session2, Arrays.asList(TOPIC_AAPL));

        // when
        Set<String> subscribers = subscriptionService.getSubscribers(TOPIC_AAPL);

        // then
        assertThat(subscribers).hasSize(2);
        assertThat(subscribers).containsExactlyInAnyOrder(session1, session2);
    }

    @Test
    @DisplayName("구독자 조회 - 구독자 없음")
    void getSubscribers_NoSubscribers() {
        // when
        Set<String> subscribers = subscriptionService.getSubscribers(TOPIC_AAPL);

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

        // when
        for (int i = 0; i < 4; i++) {
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
        assertThat(subscriptionService.shouldDisconnect("invalid")).isTrue();
    }
}