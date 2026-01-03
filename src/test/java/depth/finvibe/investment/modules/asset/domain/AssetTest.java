package depth.finvibe.investment.modules.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssetTest {

  @Test
  @DisplayName("Asset.create로 자산을 생성할 수 있다.")
  void createAsset_success() {
    // given
    Double amount = 3.5d;
    Long totalPrice = 10_000L;
    String name = "테스트자산";
    Long stockId = 123L;
    UUID userId = UUID.randomUUID();

    // when
    Asset asset = Asset.create(amount, totalPrice, name, stockId, userId);

    // then
    assertThat(asset.getAmount()).isEqualTo(amount);
    assertThat(asset.getTotalPrice()).isEqualTo(totalPrice);
    assertThat(asset.getName()).isEqualTo(name);
    assertThat(asset.getStockId()).isEqualTo(stockId);
    assertThat(asset.getUserId()).isEqualTo(userId);
    assertThat(asset.getPortfolioGroup()).isNull();
    assertThat(asset.getId()).isNull();
  }
}
