package depth.finvibe.investment.modules.market.application;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.modules.market.application.port.out.BatchUpdatePriceRepository;
import depth.finvibe.investment.modules.market.domain.BatchUpdatePrice;

@Service
@RequiredArgsConstructor
public class BatchPriceQueryService {
    private final BatchUpdatePriceRepository batchUpdatePriceRepository;

    public List<BatchUpdatePrice> getBatchPrices(List<Long> stockIds) {
        return batchUpdatePriceRepository.findByStockIds(stockIds);
    }
}
