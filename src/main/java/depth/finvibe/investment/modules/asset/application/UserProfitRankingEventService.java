package depth.finvibe.investment.modules.asset.application;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import depth.finvibe.investment.modules.asset.application.event.AllUserProfitRatesUpdatedEvent;
import depth.finvibe.investment.modules.asset.application.port.out.UserProfitRankingData;
import depth.finvibe.investment.modules.asset.application.port.out.UserProfitRankingRepository;

@Service
@RequiredArgsConstructor
public class UserProfitRankingEventService {
    private final UserProfitRankingRepository userProfitRankingRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAllUserProfitRatesUpdatedEvent(AllUserProfitRatesUpdatedEvent event) {
        if (event == null) {
            return;
        }

        List<UserProfitRankingData> rankings = event.getRankings();
        if (rankings == null) {
            userProfitRankingRepository.replaceAllRankings(List.of());
            return;
        }

        userProfitRankingRepository.replaceAllRankings(event.getRankings());
    }
}
