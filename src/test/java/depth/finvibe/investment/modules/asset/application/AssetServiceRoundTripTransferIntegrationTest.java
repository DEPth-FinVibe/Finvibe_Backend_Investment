package depth.finvibe.investment.modules.asset.application;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import depth.finvibe.investment.config.TestRedissonConfig;
import depth.finvibe.investment.modules.asset.domain.Asset;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.domain.PortfolioGroup;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.modules.asset.infra.persistence.PortfolioGroupJpaRepository;

@SpringBootTest
@Import(TestRedissonConfig.class)
class AssetServiceRoundTripTransferIntegrationTest {

  @MockitoBean
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired
  private AssetService assetService;

  @Autowired
  private PortfolioGroupJpaRepository portfolioGroupJpaRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Test
  @DisplayName("서로 다른 트랜잭션에서 왕복 자산 이동 시 예외 없이 성공한다.")
  void transferAsset_roundTripAcrossTransactions_success() {
    UUID userId = UUID.randomUUID();
    TransactionTemplate tx = new TransactionTemplate(transactionManager);

    Long sourceId = tx.execute(status -> {
      PortfolioGroup source = PortfolioGroup.builder()
          .name("포트폴리오1")
          .userId(userId)
          .iconCode("P1")
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
      return portfolioGroupJpaRepository.save(source).getId();
    });

    Long targetId = tx.execute(status -> {
      PortfolioGroup target = PortfolioGroup.builder()
          .name("포트폴리오14")
          .userId(userId)
          .iconCode("P14")
          .assets(new ArrayList<>())
          .build();
      return portfolioGroupJpaRepository.save(target).getId();
    });

    Long sourceAssetId = tx.execute(status -> {
      PortfolioGroup source = portfolioGroupJpaRepository.findById(sourceId).orElseThrow();
      return source.getAssets().get(0).getId();
    });

    tx.executeWithoutResult(status -> assetService.transferAsset(
        sourceId,
        sourceAssetId,
        PortfolioGroupDto.TransferAssetRequest.builder().targetPortfolioId(targetId).build(),
        userId
    ));

    Long movedAssetId = tx.execute(status -> {
      PortfolioGroup movedTarget = portfolioGroupJpaRepository.findById(targetId).orElseThrow();
      return movedTarget.getAssets().get(0).getId();
    });

    assertThatCode(() -> tx.executeWithoutResult(status -> assetService.transferAsset(
        targetId,
        movedAssetId,
        PortfolioGroupDto.TransferAssetRequest.builder().targetPortfolioId(sourceId).build(),
        userId
    ))).doesNotThrowAnyException();

    tx.executeWithoutResult(status -> {
      entityManager.flush();
      entityManager.clear();
      PortfolioGroup finalSource = portfolioGroupJpaRepository.findById(sourceId).orElseThrow();
      PortfolioGroup finalTarget = portfolioGroupJpaRepository.findById(targetId).orElseThrow();

      assertThat(finalSource.getAssets()).hasSize(1);
      assertThat(finalTarget.getAssets()).isEmpty();
      assertThat(finalSource.getAssets().get(0).getId()).isEqualTo(sourceAssetId);
      assertThat(finalSource.getAssets().get(0).getStockId()).isEqualTo(100L);
    });
  }
}
