package depth.finvibe.investment.config;

import depth.finvibe.investment.modules.market.infra.client.tokenmanage.repository.TokenRepository;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * 테스트 환경용 Redis 설정
 * 실제 Redis 서버 연결 없이 Mock 객체를 사용
 */
@TestConfiguration
public class TestRedissonConfig {

  @Bean
  @Primary
  public RedissonClient redissonClient() {
    return mock(RedissonClient.class);
  }

  @Bean
  @Primary
  public RedisConnectionFactory redisConnectionFactory() {
    return mock(RedisConnectionFactory.class);
  }

  @Bean
  @Primary
  public StringRedisTemplate stringRedisTemplate() {
    return mock(StringRedisTemplate.class);
  }

  @Bean
  @Primary
  public TokenRepository tokenRepository() {
    return mock(TokenRepository.class);
  }
}
