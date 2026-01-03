package depth.finvibe.investment.modules.asset.domain.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AssetErrorCode implements DomainErrorCode {
    ONLY_OWNER_CAN_UNREGISTER_ASSET("ONLY_OWNER_CAN_UNREGISTER_ASSET", "error.asset.only_owner_can_unregister_asset"),
    ONLY_OWNER_CAN_REGISTER_ASSET("ONLY_OWNER_CAN_REGISTER_ASSET", "error.asset.only_owner_can_register_asset"),
    CANNOT_SELL_NON_EXISTENT_ASSET("CANNOT_SELL_NON_EXISTENT_ASSET", "error.asset.cannot_sell_non_existent_asset"),
    ASSET_NOT_FOUND("ASSET_NOT_FOUND", "error.asset.not_found"),
    INVALID_PORTFOLIO_GROUP_PARAMS("INVALID_PORTFOLIO_GROUP_PARAMS", "error.asset.invalid_portfolio_group_params"),
    PORTFOLIO_GROUP_NOT_FOUND("PORTFOLIO_GROUP_NOT_FOUND", "error.asset.portfolio_group_not_found"),
    NEGATIVE_MONEY_AMOUNT("NEGATIVE_MONEY_AMOUNT", "error.asset.negative_money_amount"),
    INVALID_MONEY_PARAMS("INVALID_MONEY_PARAMS", "error.asset.invalid_money_params"),
    CANNOT_ADD_DIFFERENT_CURRENCIES("CANNOT_ADD_DIFFERENT_CURRENCIES", "error.asset.cannot_add_different_currencies"),
    CANNOT_SUBTRACT_DIFFERENT_CURRENCIES("CANNOT_SUBTRACT_DIFFERENT_CURRENCIES", "error.asset.cannot_subtract_different_currencies");

    private final String code;
    private final String messageKey;
}
