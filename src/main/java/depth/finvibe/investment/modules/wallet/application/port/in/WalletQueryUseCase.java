package depth.finvibe.investment.modules.wallet.application.port.in;

import depth.finvibe.investment.modules.wallet.api.dto.WalletDto;

import java.util.UUID;

public interface WalletQueryUseCase {
    WalletDto.WalletResponse getWalletByUserId(UUID userId);
}
