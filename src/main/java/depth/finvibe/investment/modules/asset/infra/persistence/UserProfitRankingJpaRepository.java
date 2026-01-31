package depth.finvibe.investment.modules.asset.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import depth.finvibe.investment.modules.asset.domain.UserProfitRanking;

public interface UserProfitRankingJpaRepository extends JpaRepository<UserProfitRanking, Long> {
    void deleteAll();
    
    Optional<UserProfitRanking> findByUserId(UUID userId);
    
    List<UserProfitRanking> findAllByOrderByRankAsc();
}
