package depth.finvibe.investment.modules.asset.infra.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceClient {
    private static final ParameterizedTypeReference<Map<UUID, String>> mapOfUuidToStringTypeRef = new ParameterizedTypeReference<>() {};
    private final RestClient restClient = RestClient.builder()
            .baseUrl("http://user")
            .build();

    public Map<UUID, String> getUserNicknamesByIds(Iterable<UUID> userIds) {
        List<UUID> ids = new ArrayList<>();
        userIds.forEach(ids::add);

        if (ids.isEmpty()) {
            return Map.of();
        }

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/members/nicknames")
                        .queryParam("userIds", ids.toArray())
                        .build())
                .retrieve()
                .body(mapOfUuidToStringTypeRef);
    }


}
