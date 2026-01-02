package depth.finvibe.investment.modules.wallet.dto;

import depth.finvibe.investment.modules.wallet.domain.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class WalletDto {

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class WalletResponse {
        private Long walletId;
        private UUID userId;
        private Long balance;

        public static WalletResponse from(Wallet wallet) {
            return WalletResponse.builder()
                    .walletId(wallet.getId())
                    .userId(wallet.getUserId())
                    .balance(wallet.getBalance().getAmount())
                    .build();
        }
    }
}
