package depth.finvibe.investment.modules.trade.domain.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TradeErrorCode implements DomainErrorCode {

    ALREADY_CANCELLED_TRADE("ALREADY_CANCELLED_TRADE", "trade.error.alreadyCancelledTrade"),
    TRADE_NOT_FOUND("TRADE_NOT_FOUND", "trade.error.tradeNotFound"),
    RESERVED_TRADE_ONLY_CANCELLABLE("RESERVED_TRADE_ONLY_CANCELLABLE", "trade.error.reservedTradeOnlyCancellable"),
    INVALID_TRADE_TYPE("INVALID_TRADE_TYPE", "trade.error.invalidTradeType"),
    INVALID_TRADE_ID_FORMAT("INVALID_TRADE_ID_FORMAT", "trade.error.invalidTradeIdFormat")
    ;


    private final String code;
    private final String messageKey;
}
