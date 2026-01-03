package depth.finvibe.investment.modules.asset.domain.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AssetErrorCode implements DomainErrorCode {
    ASSET_NOT_FOUND("ASSET_NOT_FOUND", "error.asset.not_found"),
    INVALID_PORTFOLIO_GROUP_PARAMS("INVALID_PORTFOLIO_GROUP_PARAMS", "error.asset.invalid_portfolio_group_params"),
    PORTFOLIO_GROUP_NOT_FOUND("PORTFOLIO_GROUP_NOT_FOUND", "error.asset.portfolio_group_not_found")
    ;

    private final String code;
    private final String messageKey;
}
