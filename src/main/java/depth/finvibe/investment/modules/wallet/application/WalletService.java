package depth.finvibe.investment.modules.wallet.application;

import depth.finvibe.investment.modules.wallet.domain.Wallet;
import depth.finvibe.investment.modules.wallet.domain.Money;
import depth.finvibe.investment.modules.wallet.domain.error.WalletErrorCode;
import depth.finvibe.investment.modules.wallet.infra.WalletRepository;
import depth.finvibe.investment.shared.error.DomainException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;

    @Transactional
    public Wallet createWallet(UUID userId) {
        Wallet wallet = Wallet.create(userId);
        return walletRepository.save(wallet);
    }

    @Transactional
    public void deposit(Long walletId, Long amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new DomainException(WalletErrorCode.WALLET_NOT_FOUND));
        wallet.deposit(new Money(amount));
        walletRepository.save(wallet);
    }

    @Transactional
    public void withdraw(Long walletId, Long amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new DomainException(WalletErrorCode.WALLET_NOT_FOUND));

        validateSufficientBalance(wallet, amount);

        wallet.withdraw(new Money(amount));
        walletRepository.save(wallet);
    }

    private void validateSufficientBalance(Wallet wallet, Long amount) {
        if (wallet.getBalance().getAmount() < amount) {
            throw new DomainException(WalletErrorCode.INSUFFICIENT_BALANCE);
        }
    }

}