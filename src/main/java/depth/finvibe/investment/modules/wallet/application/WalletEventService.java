package depth.finvibe.investment.modules.wallet.application;

import depth.finvibe.investment.modules.wallet.application.port.in.WalletCommandUseCase;
import depth.finvibe.investment.shared.dto.SignUpEvent;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletEventService {
    private final WalletCommandUseCase commandUseCase;

    @Transactional
    public void handleTradeExecutedEvent(TradeExecutedEvent event) {
        UUID userId = UUID.fromString(event.getUserId());

        if (event.getType().equals("BUY")) {
            commandUseCase.withdraw(userId, event.getPrice().longValue());
        } else if (event.getType().equals("SELL")) {
            commandUseCase.deposit(userId, event.getPrice().longValue());
        } else {
            log.warn("Ignoring trade event of type: {}", event.getType());
        }
    }

    @Transactional
    public void handleSignUpEvent(SignUpEvent event) {
        UUID userId = UUID.fromString(event.getUserId());
        commandUseCase.createWallet(userId);
    }
}
