package depth.finvibe.investment.modules.wallet.application.port.in;

import depth.finvibe.investment.modules.wallet.api.dto.WalletDto;

import java.util.UUID;

public interface WalletCommandUseCase {
    WalletDto.WalletResponse createWallet(UUID userId);
    WalletDto.WalletResponse deposit(UUID userId, Long amount);
    WalletDto.WalletResponse withdraw(UUID userId, Long amount);
}
