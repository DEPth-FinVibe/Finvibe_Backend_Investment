package depth.finvibe.investment.modules.asset.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import depth.finvibe.investment.modules.asset.domain.UserProfitRanking;
import depth.finvibe.investment.modules.asset.domain.enums.UserProfitRankType;

public interface UserProfitRankingQueryRepository {
  Page<UserProfitRanking> findByRankType(UserProfitRankType rankType, Pageable pageable);
}
