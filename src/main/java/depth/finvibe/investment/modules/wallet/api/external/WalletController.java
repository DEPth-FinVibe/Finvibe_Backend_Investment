package depth.finvibe.investment.modules.wallet.api.external;

import depth.finvibe.investment.boot.security.model.AuthenticatedUser;
import depth.finvibe.investment.boot.security.model.Requester;
import depth.finvibe.investment.modules.wallet.application.port.in.WalletQueryUseCase;
import depth.finvibe.investment.modules.wallet.dto.WalletDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {
    private final WalletQueryUseCase queryUseCase;

    @GetMapping("/balance")
    public WalletDto.WalletResponse getBalanceByUserId(
            @AuthenticatedUser Requester requester
    ) {
        return queryUseCase.getWalletByUserId(requester.getUuid());
    }
}
