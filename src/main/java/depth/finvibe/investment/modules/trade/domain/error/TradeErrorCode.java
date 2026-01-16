package depth.finvibe.investment.modules.trade.domain.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TradeErrorCode implements DomainErrorCode {

    ALREADY_CANCELLED_TRADE("ALREADY_CANCELLED_TRADE", "이미 취소된 거래입니다."),
    TRADE_NOT_FOUND("TRADE_NOT_FOUND", "거래를 찾을 수 없습니다."),
    RESERVED_TRADE_ONLY_CANCELLABLE("RESERVED_TRADE_ONLY_CANCELLABLE", "예약 상태의 거래만 취소할 수 있습니다."),
    INVALID_TRADE_TYPE("INVALID_TRADE_TYPE", "유효하지 않은 거래 유형입니다."),
    INVALID_TRADE_ID_FORMAT("INVALID_TRADE_ID_FORMAT", "거래 ID 형식이 올바르지 않습니다."),
    CANNOT_CANCEL_NON_RESERVED_TRADE("CANNOT_CANCEL_NON_RESERVED_TRADE", "예약 상태가 아닌 거래는 취소할 수 없습니다.")
    ;


    private final String code;
    private final String message;
}
