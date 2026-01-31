package depth.finvibe.investment.modules.asset.application.port.in;

import org.springframework.data.domain.Pageable;

import depth.finvibe.investment.modules.asset.domain.enums.UserProfitRankType;
import depth.finvibe.investment.modules.asset.dto.UserProfitRankingDto;

public interface UserProfitRankingQueryUseCase {
  UserProfitRankingDto.RankingPageResponse getUserProfitRankings(
    UserProfitRankType rankType,
    Pageable pageable
  );
}
