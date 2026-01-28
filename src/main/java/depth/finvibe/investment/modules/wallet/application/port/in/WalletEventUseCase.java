package depth.finvibe.investment.modules.wallet.application.port.in;

import depth.finvibe.investment.shared.dto.SignUpEvent;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;

public interface WalletEventUseCase {
    void handleTradeExecutedEvent(TradeExecutedEvent event);

    void handleSignUpEvent(SignUpEvent event);
}
