package depth.finvibe.investment.modules.market.dto;

import depth.finvibe.investment.modules.market.domain.error.WebSocketErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class SubscriptionDto {

    @Getter
    @Builder
    public static class Result {
        private final boolean success;
        private final List<String> subscribed;
        private final List<String> alreadySubscribed;
        private final List<String> rejected;
        private final WebSocketErrorCode errorCode;

        public static Result success(List<String> subscribed, List<String> alreadySubscribed, List<String> rejected) {
            return Result.builder()
                    .success(true)
                    .subscribed(subscribed)
                    .alreadySubscribed(alreadySubscribed)
                    .rejected(rejected)
                    .errorCode(null)
                    .build();
        }

        public static Result error(WebSocketErrorCode errorCode) {
            return Result.builder()
                    .success(false)
                    .subscribed(List.of())
                    .alreadySubscribed(List.of())
                    .rejected(List.of())
                    .errorCode(errorCode)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class UnsubscribeResult {
        private final boolean success;
        private final List<String> unsubscribed;
        private final List<String> notSubscribed;
        private final WebSocketErrorCode errorCode;

        public static UnsubscribeResult success(List<String> unsubscribed, List<String> notSubscribed) {
            return UnsubscribeResult.builder()
                    .success(true)
                    .unsubscribed(unsubscribed)
                    .notSubscribed(notSubscribed)
                    .errorCode(null)
                    .build();
        }

        public static UnsubscribeResult error(WebSocketErrorCode errorCode) {
            return UnsubscribeResult.builder()
                    .success(false)
                    .unsubscribed(List.of())
                    .notSubscribed(List.of())
                    .errorCode(errorCode)
                    .build();
        }
    }
}