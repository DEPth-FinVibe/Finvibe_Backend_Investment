package depth.finvibe.investment.shared.application.port.out;

import depth.finvibe.investment.shared.dto.RewardBadgeEvent;
import depth.finvibe.investment.shared.dto.UserMetricUpdatedEvent;

public interface GamificationEventProducer {
  void publishUserMetricUpdatedEvent(UserMetricUpdatedEvent event);

  void publishRewardBadgeEvent(RewardBadgeEvent event);
}
