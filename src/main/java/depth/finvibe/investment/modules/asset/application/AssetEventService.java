package depth.finvibe.investment.modules.asset.application;

import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.dto.FirstLoginedEvent;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.modules.asset.dto.TradeExecutedEvent;
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

        Long portfolioId =event.portfolioId();
        UUID userId = UUID.fromString(event.userId());

        if (event.type().equals("BUY")) {
            PortfolioGroupDto.RegisterAssetRequest request = createRegisterRequestFrom(event);
            commandUseCase.registerAsset(portfolioId,request, userId);
        } else if (event.type().equals("SELL")) {
            PortfolioGroupDto.UnregisterAssetRequest request = createUnregisterRequestFrom(event);
            commandUseCase.unregisterAsset(portfolioId, request, userId);
        } else {
            log.warn("Ignoring trade event of type: {}", event.type());
        }

    }

    @Transactional
    public void handleFirstLoginedEvent(FirstLoginedEvent event) {
        UUID userId = UUID.fromString(event.userId());
        commandUseCase.createDefaultPortfolioGroup(userId);
    }

    private PortfolioGroupDto.RegisterAssetRequest createRegisterRequestFrom(TradeExecutedEvent event) {
        return PortfolioGroupDto.RegisterAssetRequest.builder()
                .stockId(event.stockId())
                .name(event.name())
                .stockPrice(event.stockPrice())
                .amount(event.amount())
                .currency(event.currency())
                .build();
    }

    private PortfolioGroupDto.UnregisterAssetRequest createUnregisterRequestFrom(TradeExecutedEvent event) {
        return PortfolioGroupDto.UnregisterAssetRequest.builder()
                .stockId(event.stockId())
                .stockPrice(event.stockPrice())
                .amount(event.amount())
                .currency(event.currency())
                .build();
    }
}
