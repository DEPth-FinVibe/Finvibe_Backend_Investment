package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.HoldingStockRepository;
import depth.finvibe.investment.modules.market.domain.HoldingStock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class HoldingStockRepositoryImpl implements HoldingStockRepository {
    private final HoldingStockJpaRepository jpaRepository;

    @Override
    @Transactional
    public void registerHoldingStock(Long stockId, UUID userId) {
        jpaRepository.findByStockIdAndUserId(stockId, userId)
                .orElseGet(() -> jpaRepository.save(HoldingStock.create(stockId, userId)));
    }

    @Override
    @Transactional
    public void unregisterHoldingStock(Long stockId, UUID userId) {
        jpaRepository.findByStockIdAndUserId(stockId, userId)
                .ifPresent(jpaRepository::delete);
    }
}

