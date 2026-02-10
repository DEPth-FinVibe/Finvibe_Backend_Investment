package depth.finvibe.investment.modules.asset.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import depth.finvibe.investment.modules.asset.application.port.out.UserProfitRankingQueryRepository;
import depth.finvibe.investment.modules.asset.domain.UserProfitRanking;
import depth.finvibe.investment.modules.asset.domain.enums.UserProfitRankType;
import depth.finvibe.investment.shared.application.port.out.GamificationEventProducer;
import depth.finvibe.investment.shared.dto.Badge;
import depth.finvibe.investment.shared.dto.RewardBadgeEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfitRankingBadgeServiceTest {

  @Mock
  private UserProfitRankingQueryRepository userProfitRankingQueryRepository;

  @Mock
  private GamificationEventProducer gamificationEventProducer;

  @InjectMocks
  private UserProfitRankingBadgeService userProfitRankingBadgeService;

  @Test
  @DisplayName("주간 랭킹 데이터가 없으면 TOP_ONE_PERCENT_TRAINER 배지를 발급하지 않는다")
  void rewardWeeklyTopOnePercentBadge_noRanking() {
    // given
    Page<UserProfitRanking> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);
    when(userProfitRankingQueryRepository.findByRankType(eq(UserProfitRankType.WEEKLY), eq(PageRequest.of(0, 1))))
      .thenReturn(emptyPage);

    // when
    userProfitRankingBadgeService.rewardWeeklyTopOnePercentBadge();

    // then
    verifyNoInteractions(gamificationEventProducer);
  }

  @Test
  @DisplayName("월간 랭킹 상위 1퍼센트 사용자에게 TOP_ONE_PERCENT_TRAINER 배지를 발급한다")
  void rewardMonthlyTopOnePercentBadge_success() {
    // given
    Page<UserProfitRanking> countPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 200);
    when(userProfitRankingQueryRepository.findByRankType(eq(UserProfitRankType.MONTHLY), eq(PageRequest.of(0, 1))))
      .thenReturn(countPage);

    List<UserProfitRanking> topRankings = List.of(
      createRanking(1),
      createRanking(2)
    );
    Page<UserProfitRanking> topPage = new PageImpl<>(topRankings, PageRequest.of(0, 2), 200);
    when(userProfitRankingQueryRepository.findByRankType(eq(UserProfitRankType.MONTHLY), eq(PageRequest.of(0, 2))))
      .thenReturn(topPage);

    // when
    userProfitRankingBadgeService.rewardMonthlyTopOnePercentBadge();

    // then
    ArgumentCaptor<RewardBadgeEvent> captor = ArgumentCaptor.forClass(RewardBadgeEvent.class);
    verify(gamificationEventProducer, times(2)).publishRewardBadgeEvent(captor.capture());
    List<RewardBadgeEvent> events = captor.getAllValues();
    assertThat(events).allSatisfy(event -> {
      assertThat(event.getBadgeCode()).isEqualTo(Badge.TOP_ONE_PERCENT_TRAINER.name());
      assertThat(event.getReason()).isEqualTo("월간 수익률 상위 1% 선정");
      assertThat(event.getUserId()).isNotBlank();
    });
  }

  private UserProfitRanking createRanking(int rank) {
    return UserProfitRanking.create(
      UUID.randomUUID(),
      "tester-" + rank,
      UserProfitRankType.MONTHLY,
      BigDecimal.valueOf(10 - rank),
      BigDecimal.valueOf(100000L - (rank * 1000L)),
      rank
    );
  }
}
