package depth.finvibe.investment.modules.trade.infra.client;

import depth.finvibe.investment.modules.asset.application.port.in.AssetQueryUseCase;
import depth.finvibe.investment.modules.trade.application.port.out.AssetClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssetClientImpl implements AssetClient {
    private final AssetQueryUseCase assetQueryUseCase;

    @Override
    public boolean isExistPortfolio(Long portfolioId, UUID userId) {
        return assetQueryUseCase.isExistPortfolio(portfolioId, userId);
    }
}
