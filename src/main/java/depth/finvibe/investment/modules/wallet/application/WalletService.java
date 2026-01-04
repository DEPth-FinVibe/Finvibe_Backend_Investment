package depth.finvibe.investment.modules.wallet.application;

import depth.finvibe.investment.modules.wallet.application.port.in.WalletCommandUseCase;
import depth.finvibe.investment.modules.wallet.application.port.in.WalletQueryUseCase;
import depth.finvibe.investment.modules.wallet.application.port.out.WalletRepository;
import depth.finvibe.investment.modules.wallet.domain.Wallet;
import depth.finvibe.investment.modules.wallet.domain.Money;
import depth.finvibe.investment.modules.wallet.domain.error.WalletErrorCode;
import depth.finvibe.investment.modules.wallet.dto.WalletDto;
import depth.finvibe.investment.shared.error.DomainException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService implements WalletCommandUseCase, WalletQueryUseCase {
    private final WalletRepository walletRepository;

    @Transactional
    public WalletDto.WalletResponse createWallet(UUID userId) {
        if (userId == null) {
            throw new DomainException(WalletErrorCode.INVALID_USER_ID);
        }

        Wallet wallet = Wallet.create(userId);
        Wallet saved = walletRepository.save(wallet);

        return WalletDto.WalletResponse.from(saved);
    }

    @Transactional
    public WalletDto.WalletResponse deposit(UUID userId, Long price) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new DomainException(WalletErrorCode.WALLET_NOT_FOUND));

        validatePrice(price);
        wallet.deposit(new Money(price));

        return WalletDto.WalletResponse.from(wallet);
    }

    @Transactional
    public WalletDto.WalletResponse withdraw(UUID userId, Long price) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new DomainException(WalletErrorCode.WALLET_NOT_FOUND));

        validatePrice(price);
        wallet.withdraw(new Money(price));

        return WalletDto.WalletResponse.from(wallet);
    }

    private void validatePrice(Long price) {
        if (price == null || price <= 0) {
            throw new DomainException(WalletErrorCode.INVALID_MONEY_PRICE);
        }
    }

    @Override
    public WalletDto.WalletResponse getWalletByUserId(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new DomainException(WalletErrorCode.WALLET_NOT_FOUND));

        return WalletDto.WalletResponse.from(wallet);
    }
}