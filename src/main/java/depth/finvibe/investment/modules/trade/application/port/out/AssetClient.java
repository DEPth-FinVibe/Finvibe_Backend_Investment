package depth.finvibe.investment.modules.trade.application.port.out;

import java.util.UUID;

public interface AssetClient {
    boolean isExistPortfolio(Long portfolioId, UUID userId);
}
