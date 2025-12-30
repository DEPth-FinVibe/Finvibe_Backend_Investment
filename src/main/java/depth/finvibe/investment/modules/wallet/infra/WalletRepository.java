package depth.finvibe.investment.modules.wallet.infra;

import depth.finvibe.investment.modules.wallet.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
}
