package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.HoldingStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class HoldingStockRepositoryImpl implements HoldingStockRepository {
    @Override
    public void registerHoldingStock(Long stockId, UUID userId) {

    }

    @Override
    public void unregisterHoldingStock(Long stockId, UUID userId) {

    }
}
