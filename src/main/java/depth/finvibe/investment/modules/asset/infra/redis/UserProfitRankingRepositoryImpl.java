package depth.finvibe.investment.modules.asset.infra.redis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import depth.finvibe.investment.modules.asset.application.port.out.UserProfitRankingRepository;

@Repository
@RequiredArgsConstructor
public class UserProfitRankingRepositoryImpl implements UserProfitRankingRepository {
  private static final String RANKING_KEY = "ranking:user:profit_rate";

  private final StringRedisTemplate redisTemplate;

  @Override
  public void update(UUID userId, BigDecimal totalReturnRate) {
    if (userId == null || totalReturnRate == null) {
      return;
    }

    double score = totalReturnRate.setScale(4, RoundingMode.HALF_UP).doubleValue();
    redisTemplate.opsForZSet().add(RANKING_KEY, userId.toString(), score);
  }

  @Override
  public void remove(UUID userId) {
    if (userId == null) {
      return;
    }

    redisTemplate.opsForZSet().remove(RANKING_KEY, userId.toString());
  }
}
