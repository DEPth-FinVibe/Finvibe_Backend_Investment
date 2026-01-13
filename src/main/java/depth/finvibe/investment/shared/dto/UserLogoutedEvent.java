package depth.finvibe.investment.shared.dto;

import java.util.List;

public record UserLogoutedEvent (
    String userId,
    List<Long> interestedStockIds,
    List<Long> ownedStockIds
){}