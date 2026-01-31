package depth.finvibe.investment.modules.asset.application.port.out;

import java.util.List;

public interface UserProfitRankingRepository {
  void replaceAllRankings(List<UserProfitRankingData> rankings);
}
