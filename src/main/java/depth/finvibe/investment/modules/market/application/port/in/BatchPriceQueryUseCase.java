package depth.finvibe.investment.modules.market.application.port.in;

import java.util.List;

import depth.finvibe.investment.shared.dto.BatchPriceSnapshot;

public interface BatchPriceQueryUseCase {
    List<BatchPriceSnapshot> getBatchPrices(List<Long> stockIds);
}
