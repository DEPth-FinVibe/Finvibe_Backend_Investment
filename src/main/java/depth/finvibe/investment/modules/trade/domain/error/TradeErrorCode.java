package depth.finvibe.investment.modules.trade.domain.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TradeErrorCode implements DomainErrorCode {

    ALREADY_CANCELLED_TRADE("ALREADY_CANCELLED_TRADE", "trade.error.alreadyCancelledTrade"),
    CANNOT_CANCEL_NON_RESERVED_TRADE("CANNOT_CANCEL_NON_RESERVED_TRADE", "trade.error.cannotCancelNonReservedTrade")
    ;


    private final String code;
    private final String messageKey;
}
