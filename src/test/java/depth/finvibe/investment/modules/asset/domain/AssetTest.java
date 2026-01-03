package depth.finvibe.investment.modules.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import depth.finvibe.investment.modules.asset.domain.Money;
import depth.finvibe.investment.modules.asset.domain.Currency;

class AssetTest {

  @Test
  @DisplayName("Asset.create로 자산을 생성할 수 있다.")
  void createAsset_success() {
    // given
    Double amount = 3.5d;
    Money totalPrice = Money.of(10_000d, Currency.KRW);
    String name = "테스트자산";
    Long stockId = 123L;
    UUID userId = UUID.randomUUID();

    // when
    Asset asset = Asset.create(amount, totalPrice, name, stockId, userId);

    // then
    assertThat(asset.getAmount()).isEqualTo(amount);
    assertThat(asset.getTotalPrice().getAmount()).isEqualTo(totalPrice.getAmount());
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
    Asset asset = Asset.create(1.0, Money.of(10_000d, Currency.KRW), "자산", 1L, userId);

    // when
    asset.additionalBuy(0.5, Money.of(5_000d, Currency.KRW));

    // then
    assertThat(asset.getAmount()).isEqualTo(1.5d);
    assertThat(asset.getTotalPrice().getAmount()).isEqualTo(15_000d);
    assertThat(asset.getTotalPrice().getCurrency()).isEqualTo(Currency.KRW);
  }

  @Test
  @DisplayName("부분 매도 시 수량과 총액이 차감된다.")
  void partialSell_success() {
    // given
    UUID userId = UUID.randomUUID();
    Asset asset = Asset.create(2.0, Money.of(20_000d, Currency.KRW), "자산", 1L, userId);

    // when
    asset.partialSell(0.5, Money.of(5_000d, Currency.KRW));

    // then
    assertThat(asset.getAmount()).isEqualTo(1.5d);
    assertThat(asset.getTotalPrice().getAmount()).isEqualTo(15_000d);
    assertThat(asset.getTotalPrice().getCurrency()).isEqualTo(Currency.KRW);
  }
}
