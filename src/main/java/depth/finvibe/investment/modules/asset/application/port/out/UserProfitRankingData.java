package depth.finvibe.investment.modules.asset.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public record UserProfitRankingData(
    UUID userId,
    BigDecimal totalReturnRate,
    BigDecimal totalProfitLoss
) {}
