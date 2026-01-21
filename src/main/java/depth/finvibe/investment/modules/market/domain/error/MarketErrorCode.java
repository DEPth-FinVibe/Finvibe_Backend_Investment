package depth.finvibe.investment.modules.market.domain.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MarketErrorCode implements DomainErrorCode {

    INVALID_CATEGORY_NAME("MARKET_INVALID_CATEGORY_NAME", "유효하지 않은 카테고리 이름입니다."),
    STOCK_NOT_FOUND("MARKET_STOCK_NOT_FOUND", "해당 종목을 찾을 수 없습니다."),
    INVALID_TIME_RANGE("MARKET_INVALID_TIME_RANGE", "종료 시각은 현재 시각보다 미래일 수 없습니다."),
    INVALID_START_END_TIME("MARKET_INVALID_START_END_TIME", "시작 시각은 종료 시각보다 이전이어야 합니다.");

    private final String code;
    private final String message;
}
