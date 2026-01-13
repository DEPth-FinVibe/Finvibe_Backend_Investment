package depth.finvibe.investment.modules.asset.application;

import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
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
public class AssetEventService {
    private final AssetCommandUseCase commandUseCase;

    @Transactional
    public void handleTradeExecutedEvent(TradeExecutedEvent event) {

        Long portfolioId = event.getPortfolioId();
        UUID userId = UUID.fromString(event.getUserId());

        if (event.getType().equals("BUY")) {
            PortfolioGroupDto.RegisterAssetRequest request = createRegisterRequestFrom(event);
            commandUseCase.registerAsset(portfolioId,request, userId);
        } else if (event.getType().equals("SELL")) {
            PortfolioGroupDto.UnregisterAssetRequest request = createUnregisterRequestFrom(event);
            commandUseCase.unregisterAsset(portfolioId, request, userId);
        } else {
            log.warn("Ignoring trade event of type: {}", event.getType());
        }

    }

    @Transactional
    public void handleFirstLoginedEvent(SignUpEvent event) {
        UUID userId = UUID.fromString(event.userId());
        commandUseCase.createDefaultPortfolioGroup(userId);
    }

    private PortfolioGroupDto.RegisterAssetRequest createRegisterRequestFrom(TradeExecutedEvent event) {
        return PortfolioGroupDto.RegisterAssetRequest.builder()
                .stockId(event.getStockId())
                .name(event.getName())
                .stockPrice(event.getPrice())
                .amount(event.getAmount())
                .currency(Currency.valueOf(event.getCurrency()))
                .build();
    }

    private PortfolioGroupDto.UnregisterAssetRequest createUnregisterRequestFrom(TradeExecutedEvent event) {
        return PortfolioGroupDto.UnregisterAssetRequest.builder()
                .stockId(event.getStockId())
                .stockPrice(event.getPrice())
                .amount(event.getAmount())
                .currency(Currency.valueOf(event.getCurrency()))
                .build();
    }
}
