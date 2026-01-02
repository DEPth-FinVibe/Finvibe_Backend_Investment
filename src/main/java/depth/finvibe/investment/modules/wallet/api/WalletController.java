package depth.finvibe.investment.modules.wallet.api;

import depth.finvibe.investment.modules.wallet.application.port.in.WalletQueryUseCase;
import depth.finvibe.investment.modules.wallet.dto.WalletDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WalletController {
    private final WalletQueryUseCase queryUseCase;

    @GetMapping("/balance")
    public WalletDto.WalletResponse getBalanceByUserId(
            @RequestParam("userId") String userId
    ) {
        UUID userUuid = UUID.fromString(userId);
        return queryUseCase.getWalletByUserId(userUuid);
    }
}
