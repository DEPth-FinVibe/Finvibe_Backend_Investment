package depth.finvibe.investment.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 테스트 환경용 Redisson 설정
 * 실제 Redis 서버 연결 없이 Mock 객체를 사용
 */
@TestConfiguration
public class TestRedissonConfig {

  @Bean
  @Primary
  public RedissonClient redissonClient() {
    return mock(RedissonClient.class);
  }
}
