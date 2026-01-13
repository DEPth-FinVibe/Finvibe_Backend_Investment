package depth.finvibe.investment.shared.dto;

import java.util.List;

public record UserLoginedEvent(
    String userId,
    List<Long> interestedStockIds,
    List<Long> ownedStockIds
) {
}
