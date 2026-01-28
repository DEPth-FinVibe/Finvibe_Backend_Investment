package depth.finvibe.investment.modules.asset.application;

import java.util.UUID;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.asset.application.port.in.AssetCommandUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.AssetEventUseCase;
import depth.finvibe.investment.modules.asset.application.port.in.ProfitCalculationUseCase;
import depth.finvibe.investment.modules.asset.domain.Currency;
import depth.finvibe.investment.modules.asset.dto.PortfolioGroupDto;
import depth.finvibe.investment.shared.dto.BatchPriceUpdatedEvent;
import depth.finvibe.investment.shared.dto.SignUpEvent;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetEventService implements AssetEventUseCase {
    private final AssetCommandUseCase commandUseCase;
    private final ProfitCalculationUseCase profitCalculationService;

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
    public void handleSignUpEvent(SignUpEvent event) {
        UUID userId = UUID.fromString(event.getUserId());
        commandUseCase.createDefaultPortfolioGroup(userId);
    }

    @Transactional
    public void handleBatchPriceUpdatedEvent(BatchPriceUpdatedEvent event) {
        profitCalculationService.recalculateAllProfits(event.getUpdatedStockIds());
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
