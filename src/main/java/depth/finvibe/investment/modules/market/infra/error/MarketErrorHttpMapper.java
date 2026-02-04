package depth.finvibe.investment.modules.market.infra.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import depth.finvibe.investment.modules.market.domain.error.MarketErrorCode;
import depth.finvibe.investment.shared.error.DomainErrorCode;
import depth.finvibe.investment.shared.infra.error.DomainErrorHttpMapper;

@Component
public class MarketErrorHttpMapper implements DomainErrorHttpMapper {

    @Override
    public boolean supports(DomainErrorCode code) {
        return code instanceof MarketErrorCode;
    }

    @Override
    public HttpStatusCode toStatus(DomainErrorCode code) {
        MarketErrorCode marketCode = (MarketErrorCode) code;
        return switch (marketCode) {
            case INVALID_CATEGORY_NAME,
                 INVALID_TIME_RANGE,
                 INVALID_START_END_TIME -> HttpStatus.BAD_REQUEST;

            case CATEGORY_NOT_FOUND,
                 NO_STOCKS_IN_CATEGORY,
                 STOCK_NOT_FOUND -> HttpStatus.NOT_FOUND;

            case NO_PRICE_DATA_AVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
