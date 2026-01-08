package depth.finvibe.investment.modules.market.domain.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MarketErrorCode implements DomainErrorCode {

    INVALID_CATEGORY_NAME("MARKET_INVALID_CATEGORY_NAME", "error.market.invalid_category_name"),
    ;

    private final String code;
    private final String messageKey;
}
