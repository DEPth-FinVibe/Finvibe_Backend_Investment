package depth.finvibe.investment.modules.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssetTest {

  @Test
  @DisplayName("Asset.create로 자산을 생성할 수 있다.")
  void createAsset_success() {
    // given
    BigDecimal amount = BigDecimal.valueOf(3.5d);
    BigDecimal unitPrice = BigDecimal.valueOf(10_000d);
    Currency currency = Currency.KRW;
    String name = "테스트자산";
    Long stockId = 123L;
    UUID userId = UUID.randomUUID();

    // when
    Asset asset = Asset.create(amount, unitPrice, currency, name, stockId, userId);

    // then
    assertThat(asset.getAmount()).isEqualByComparingTo(amount);
    assertThat(asset.getTotalPrice().getAmount()).isEqualByComparingTo(unitPrice.multiply(amount));
    assertThat(asset.getTotalPrice().getCurrency()).isEqualTo(Currency.KRW);
    assertThat(asset.getName()).isEqualTo(name);
    assertThat(asset.getStockId()).isEqualTo(stockId);
    assertThat(asset.getUserId()).isEqualTo(userId);
    assertThat(asset.getPortfolioGroup()).isNull();
    assertThat(asset.getId()).isNull();
  }

  @Test
  @DisplayName("추가 매수 시 수량과 총액이 누적된다.")
  void additionalBuy_success() {
    // given
    UUID userId = UUID.randomUUID();
    Asset asset = Asset.create(BigDecimal.valueOf(1.0), BigDecimal.valueOf(10_000d), Currency.KRW, "자산", 1L, userId);

    // when
    asset.additionalBuy(BigDecimal.valueOf(0.5), Money.of(5_000d, Currency.KRW));

    // then
    assertThat(asset.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1.5d));
    assertThat(asset.getTotalPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(15_000d));
    assertThat(asset.getTotalPrice().getCurrency()).isEqualTo(Currency.KRW);
  }

  @Test
  @DisplayName("부분 매도 시 수량과 총액이 차감된다.")
  void partialSell_success() {
    // given
    UUID userId = UUID.randomUUID();
    Asset asset = Asset.create(BigDecimal.valueOf(2.0), BigDecimal.valueOf(10_000d), Currency.KRW, "자산", 1L, userId);

    // when
    asset.partialSell(BigDecimal.valueOf(0.5), Money.of(5_000d, Currency.KRW));

    // then
    assertThat(asset.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1.5d));
    assertThat(asset.getTotalPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(15_000d));
    assertThat(asset.getTotalPrice().getCurrency()).isEqualTo(Currency.KRW);
  }
}
