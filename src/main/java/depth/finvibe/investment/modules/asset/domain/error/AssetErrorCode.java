package depth.finvibe.investment.modules.asset.domain.error;

import depth.finvibe.investment.shared.error.DomainErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AssetErrorCode implements DomainErrorCode {
    ONLY_OWNER_CAN_UNREGISTER_ASSET("ONLY_OWNER_CAN_UNREGISTER_ASSET", "error.asset.only_owner_can_unregister_asset"),
    ONLY_OWNER_CAN_REGISTER_ASSET("ONLY_OWNER_CAN_REGISTER_ASSET", "error.asset.only_owner_can_register_asset"),
    ONLY_OWNER_CAN_VIEW_ASSETS("ONLY_OWNER_CAN_VIEW_ASSETS", "error.asset.only_owner_can_view_assets"),
    CANNOT_SELL_NON_EXISTENT_ASSET("CANNOT_SELL_NON_EXISTENT_ASSET", "error.asset.cannot_sell_non_existent_asset"),
    ASSET_NOT_FOUND("ASSET_NOT_FOUND", "error.asset.not_found"),
    INVALID_PORTFOLIO_GROUP_PARAMS("INVALID_PORTFOLIO_GROUP_PARAMS", "error.asset.invalid_portfolio_group_params"),
    PORTFOLIO_GROUP_NOT_FOUND("PORTFOLIO_GROUP_NOT_FOUND", "error.asset.portfolio_group_not_found"),
    NEGATIVE_MONEY_AMOUNT("NEGATIVE_MONEY_AMOUNT", "error.asset.negative_money_amount"),
    INVALID_MONEY_PARAMS("INVALID_MONEY_PARAMS", "error.asset.invalid_money_params"),
    CANNOT_ADD_DIFFERENT_CURRENCIES("CANNOT_ADD_DIFFERENT_CURRENCIES", "error.asset.cannot_add_different_currencies"),
    CANNOT_SUBTRACT_DIFFERENT_CURRENCIES("CANNOT_SUBTRACT_DIFFERENT_CURRENCIES", "error.asset.cannot_subtract_different_currencies"),
    CANNOT_MODIFY_DEFAULT_PORTFOLIO_GROUP("CANNOT_MODIFY_DEFAULT_PORTFOLIO_GROUP", "error.asset.cannot_modify_default_portfolio_group"),
    CANNOT_DELETE_DEFAULT_PORTFOLIO_GROUP("CANNOT_DELETE_DEFAULT_PORTFOLIO_GROUP", "error.asset.cannot_delete_default_portfolio_group"),
    ONLY_OWNER_CAN_DELETE_PORTFOLIO_GROUP("ONLY_OWNER_CAN_DELETE_PORTFOLIO_GROUP", "error.asset.only_owner_can_delete_portfolio_group"),
    DEFAULT_PORTFOLIO_GROUP_NOT_FOUND("DEFAULT_PORTFOLIO_GROUP_NOT_FOUND", "error.asset.default_portfolio_group_not_found");

    private final String code;
    private final String messageKey;
}
