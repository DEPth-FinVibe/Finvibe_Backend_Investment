package depth.finvibe.investment.modules.asset.infra.client;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import depth.finvibe.investment.shared.dto.BatchPriceSnapshot;

@Component
@RequiredArgsConstructor
public class MarketInternalClient {
    private final RestTemplate restTemplate;

    @Value("${market.internal.base-url:http://localhost:8080}")
    private String marketBaseUrl;

    public List<BatchPriceSnapshot> getBatchPrices(List<Long> stockIds) {
        String url = UriComponentsBuilder.fromUriString(marketBaseUrl)
                .path("/internal/market/batch-prices")
                .queryParam("stockIds", stockIds)
                .toUriString();

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<BatchPriceSnapshot>>() {}
        ).getBody();
    }
}
