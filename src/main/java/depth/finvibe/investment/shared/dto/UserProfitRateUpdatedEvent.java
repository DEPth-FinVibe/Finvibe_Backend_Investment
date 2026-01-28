package depth.finvibe.investment.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfitRateUpdatedEvent {
  private UUID userId;
  private BigDecimal totalReturnRate;
  private boolean hasAssets;
  private LocalDateTime calculatedAt;
}
