package depth.finvibe.investment.modules.asset.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import depth.finvibe.investment.modules.asset.domain.UserProfitRanking;
import depth.finvibe.investment.modules.asset.domain.enums.UserProfitRankType;

public interface UserProfitRankingJpaRepository extends JpaRepository<UserProfitRanking, Long> {
  void deleteByRankType(UserProfitRankType rankType);

  Optional<UserProfitRanking> findByRankTypeAndUserId(UserProfitRankType rankType, UUID userId);

  List<UserProfitRanking> findByRankTypeOrderByRankAsc(UserProfitRankType rankType);
}
