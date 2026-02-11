package depth.finvibe.investment.modules.asset.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.investment.config.TestRedissonConfig;
import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;

@SpringBootTest
@Import(TestRedissonConfig.class)
@Transactional
class PortfolioGroupTransferJpaTest {

  @MockitoBean
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired
  private PortfolioGroupJpaRepository portfolioGroupJpaRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  @DisplayName("자산 이동 시 대상에 동일 종목이 없어도 flush에서 예외가 발생하지 않는다.")
  void transferAsset_withoutDuplicateStock_flush_success() {
    UUID userId = UUID.randomUUID();
    PortfolioGroup source = PortfolioGroup.builder()
        .name("원본")
        .userId(userId)
        .iconCode("SOURCE")
        .assets(new ArrayList<>())
        .build();
    source.register(Asset.create(
        BigDecimal.valueOf(2),
        BigDecimal.valueOf(10_000),
        Currency.KRW,
        "삼성전자",
        100L,
        userId
    ), userId);

    PortfolioGroup target = PortfolioGroup.builder()
        .name("대상")
        .userId(userId)
        .iconCode("TARGET")
        .assets(new ArrayList<>())
        .build();

    source = portfolioGroupJpaRepository.save(source);
    target = portfolioGroupJpaRepository.save(target);
    entityManager.flush();
    entityManager.clear();

    PortfolioGroup loadedSource = portfolioGroupJpaRepository.findById(source.getId()).orElseThrow();
    PortfolioGroup loadedTarget = portfolioGroupJpaRepository.findById(target.getId()).orElseThrow();
    Long assetId = loadedSource.getAssets().get(0).getId();

    assertThatCode(() -> {
      loadedSource.transferAssetTo(assetId, loadedTarget, userId);
      entityManager.flush();
    }).doesNotThrowAnyException();

    entityManager.clear();

    PortfolioGroup movedSource = portfolioGroupJpaRepository.findById(source.getId()).orElseThrow();
    PortfolioGroup movedTarget = portfolioGroupJpaRepository.findById(target.getId()).orElseThrow();

    assertThat(movedSource.getAssets()).isEmpty();
    assertThat(movedTarget.getAssets()).hasSize(1);
    assertThat(movedTarget.getAssets().get(0).getStockId()).isEqualTo(100L);
  }

  @Test
  @DisplayName("자산 이동 시 대상에 동일 종목이 있으면 병합되고 원본 자산은 삭제된다.")
  void transferAsset_withDuplicateStock_merge_success() {
    UUID userId = UUID.randomUUID();
    PortfolioGroup source = PortfolioGroup.builder()
        .name("원본")
        .userId(userId)
        .iconCode("SOURCE")
        .assets(new ArrayList<>())
        .build();
    source.register(Asset.create(
        BigDecimal.valueOf(2),
        BigDecimal.valueOf(5_000),
        Currency.KRW,
        "삼성전자",
        100L,
        userId
    ), userId);

    PortfolioGroup target = PortfolioGroup.builder()
        .name("대상")
        .userId(userId)
        .iconCode("TARGET")
        .assets(new ArrayList<>())
        .build();
    target.register(Asset.create(
        BigDecimal.valueOf(3),
        BigDecimal.valueOf(4_000),
        Currency.KRW,
        "삼성전자",
        100L,
        userId
    ), userId);

    source = portfolioGroupJpaRepository.save(source);
    target = portfolioGroupJpaRepository.save(target);
    entityManager.flush();
    entityManager.clear();

    PortfolioGroup loadedSource = portfolioGroupJpaRepository.findById(source.getId()).orElseThrow();
    PortfolioGroup loadedTarget = portfolioGroupJpaRepository.findById(target.getId()).orElseThrow();
    Long assetId = loadedSource.getAssets().get(0).getId();

    loadedSource.transferAssetTo(assetId, loadedTarget, userId);
    entityManager.flush();
    entityManager.clear();

    PortfolioGroup mergedSource = portfolioGroupJpaRepository.findById(source.getId()).orElseThrow();
    PortfolioGroup mergedTarget = portfolioGroupJpaRepository.findById(target.getId()).orElseThrow();

    assertThat(mergedSource.getAssets()).isEmpty();
    assertThat(mergedTarget.getAssets()).hasSize(1);
    assertThat(mergedTarget.getAssets().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5));
    assertThat(mergedTarget.getAssets().get(0).getTotalPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(22_000));
  }
}
