package depth.finvibe.investment.modules.wallet.application.port.out;

import java.util.Optional;
import java.util.UUID;

import depth.finvibe.investment.modules.wallet.domain.Wallet;

public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
