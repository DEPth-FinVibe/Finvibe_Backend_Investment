package depth.finvibe.investment.modules.trade.application.port.out;

import java.util.UUID;

public interface WalletClient {
    Long getWalletBalance(UUID userId);
}
