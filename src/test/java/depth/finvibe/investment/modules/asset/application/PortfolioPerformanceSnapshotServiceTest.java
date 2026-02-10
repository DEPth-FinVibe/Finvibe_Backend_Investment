package depth.finvibe.investment.modules.asset.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import depth.finvibe.investment.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.investment.modules.asset.application.port.out.PortfolioPerformanceSnapshotRepository;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.domain.PortfolioPerformanceSnapshotDaily;
import depth.finvibe.investment.modules.asset.domain.PortfolioValuation;

@ExtendWith(MockitoExtension.class)
class PortfolioPerformanceSnapshotServiceTest {

  @Mock
  private PortfolioGroupRepository portfolioGroupRepository;

  @Mock
  private PortfolioPerformanceSnapshotRepository portfolioPerformanceSnapshotRepository;

  @InjectMocks
  private PortfolioPerformanceSnapshotService portfolioPerformanceSnapshotService;

  @Test
  @DisplayName("일별 포트폴리오 성과 스냅샷을 저장한다.")
  void saveDailySnapshot_success() {
    UUID userId = UUID.randomUUID();
    PortfolioGroup growth = PortfolioGroup.builder()
      .id(1L)
      .name("성장형")
      .userId(userId)
      .valuation(PortfolioValuation.builder()
        .totalCurrentValue(new BigDecimal("1200000"))
        .totalProfitLoss(new BigDecimal("150000"))
        .totalReturnRate(new BigDecimal("14.2900"))
        .build())
      .build();

    PortfolioGroup safety = PortfolioGroup.builder()
      .id(2L)
      .name("안정형")
      .userId(userId)
      .valuation(PortfolioValuation.builder()
        .totalCurrentValue(new BigDecimal("800000"))
        .totalProfitLoss(new BigDecimal("30000"))
        .totalReturnRate(new BigDecimal("3.9000"))
        .build())
      .build();

    when(portfolioGroupRepository.findAllWithAssets()).thenReturn(List.of(growth, safety));

    portfolioPerformanceSnapshotService.saveDailySnapshot(LocalDate.of(2026, 2, 9));

    ArgumentCaptor<List<PortfolioPerformanceSnapshotDaily>> captor = ArgumentCaptor.forClass(List.class);
    verify(portfolioPerformanceSnapshotRepository).saveAll(captor.capture());
    List<PortfolioPerformanceSnapshotDaily> saved = captor.getValue();

    org.assertj.core.api.Assertions.assertThat(saved).hasSize(2);
    org.assertj.core.api.Assertions.assertThat(saved.get(0).getId().getSnapshotDate()).isEqualTo(LocalDate.of(2026, 2, 9));
    org.assertj.core.api.Assertions.assertThat(saved.get(0).getPortfolioName()).isEqualTo("성장형");
    org.assertj.core.api.Assertions.assertThat(saved.get(0).getTotalCurrentValue()).isEqualByComparingTo("1200000");
  }
}
