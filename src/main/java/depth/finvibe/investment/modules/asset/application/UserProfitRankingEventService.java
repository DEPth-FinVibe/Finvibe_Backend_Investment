package depth.finvibe.investment.modules.asset.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.asset.application.port.out.UserProfitRankingRepository;
import depth.finvibe.investment.shared.dto.UserProfitRateUpdatedEvent;

@Service
@RequiredArgsConstructor
public class UserProfitRankingEventService {
  private final UserProfitRankingRepository userProfitRankingRepository;

  @EventListener
  public void handleUserProfitRateUpdatedEvent(UserProfitRateUpdatedEvent event) {
    if (event == null || event.getUserId() == null) {
      return;
    }

    if (event.isHasAssets()) {
      userProfitRankingRepository.update(event.getUserId(), event.getTotalReturnRate());
      return;
    }

    userProfitRankingRepository.remove(event.getUserId());
  }
}
