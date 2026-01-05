package depth.finvibe.investment.modules.wallet.application;

import depth.finvibe.investment.modules.wallet.application.port.in.WalletCommandUseCase;
import depth.finvibe.investment.shared.dto.FirstLoginedEvent;
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
        UUID userId = UUID.fromString(event.userId());

        if (event.type().equals("BUY")) {
            commandUseCase.withdraw(userId, event.price());
        } else if (event.type().equals("SELL")) {
            commandUseCase.deposit(userId, event.price());
        } else {
            log.warn("Ignoring trade event of type: {}", event.type());
        }
    }

    @Transactional
    public void handleFirstLoginedEvent(FirstLoginedEvent event) {
        UUID userId = UUID.fromString(event.userId());
        commandUseCase.createWallet(userId);
    }
}
