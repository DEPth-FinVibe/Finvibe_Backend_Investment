package depth.finvibe.investment.modules.asset.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface UserProfitRankingRepository {
  void update(UUID userId, BigDecimal totalReturnRate);

  void remove(UUID userId);
}
