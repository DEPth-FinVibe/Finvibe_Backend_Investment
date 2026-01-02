package depth.finvibe.investment.modules.wallet.infra.persistence;

import depth.finvibe.investment.modules.wallet.application.port.out.WalletRepository;
import depth.finvibe.investment.modules.wallet.domain.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {
    private final WalletJpaRepository jpaRepository;

    @Override
    public Wallet save(Wallet wallet) {
        return jpaRepository.save(wallet);
    }

    @Override
    public Optional<Wallet> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.existsByUserId(userId);
    }
}
