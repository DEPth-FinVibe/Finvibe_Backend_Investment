package depth.finvibe.investment.modules.market.infra.redis;

import depth.finvibe.investment.modules.market.application.port.out.CurrentPriceRepository;
import depth.finvibe.investment.modules.market.domain.CurrentPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class CurrentPriceRepositoryImpl implements CurrentPriceRepository {

    private static final String KEY_PREFIX = "market:current-price:";
    private static final Duration CURRENT_PRICE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void upsertCurrentPrice(CurrentPrice currentPrice) {
        try {
            String value = objectMapper.writeValueAsString(currentPrice);
            redisTemplate.opsForValue().set(keyForStock(currentPrice.getStockId()), value, CURRENT_PRICE_TTL);
        } catch (JacksonIOException ex) {
            throw new IllegalStateException("Failed to serialize current price", ex);
        }
    }

    @Override
    public void deleteCurrentPrice(Long stockId) {
        redisTemplate.delete(keyForStock(stockId));
    }

    @Override
    public List<CurrentPrice> findByStockIds(List<Long> stockIds) {
        List<String> keys = stockIds.stream()
                .map(this::keyForStock)
                .toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .flatMap(this::deserializeSafely)
                .toList();
    }

    private String keyForStock(Long stockId) {
        return KEY_PREFIX + stockId;
    }

    private Stream<CurrentPrice> deserializeSafely(String value) {
        try {
            return Stream.of(objectMapper.readValue(value, CurrentPrice.class));
        } catch (JacksonIOException ex) {
            throw new IllegalStateException("Failed to deserialize current price", ex);
        }
    }
}

