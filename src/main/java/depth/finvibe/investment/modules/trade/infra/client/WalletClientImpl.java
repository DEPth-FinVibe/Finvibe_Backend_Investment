package depth.finvibe.investment.modules.trade.infra.client;

import depth.finvibe.investment.modules.trade.application.port.out.WalletClient;
import depth.finvibe.investment.modules.wallet.application.port.in.WalletQueryUseCase;
import depth.finvibe.investment.modules.wallet.dto.WalletDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletClientImpl implements WalletClient {

    private final WalletQueryUseCase walletQueryUseCase;

    @Override
    public Long getWalletBalance(UUID userId) {
        WalletDto.WalletResponse wallet = walletQueryUseCase.getWalletByUserId(userId);
        return wallet.getBalance();
    }
}
