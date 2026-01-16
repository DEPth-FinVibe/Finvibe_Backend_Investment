package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.modules.market.dto.StockDto;

import java.util.List;
import java.math.BigDecimal;

public interface MarketCommandUseCase {

    void updateCurrentPrices(List<Long> stockIds);

    void registerNewStock(StockDto.NewStock request);

    void updateStockHoldingAmount(Long stockId, BigDecimal deltaAmount);

    void addRegionOfInterestLevel1(List<Long> interestStockIds);

    void addRegionOfInterestLevel2(List<Long> ownedStockIds);

    void removeRegionOfInterestLevel1(List<Long> interestStockIds);

    void removeRegionOfInterestLevel2(List<Long> ownedStockIds);

    // -> 실시간 주가 데이터 말고 과거 주가데이터 조회 정책이 없음 (캐싱정책)
    // -> 누군가가 어떤 stockId에 대해 (2025-01-01 12:00부터 20개의 분봉 데이터) 조회를 요청함
    // -> 이중에서 가지고있는데이터와 없는데이터를 파악
    // -> 가지고있는건 그냥 주고(cache hit), 없으면 외부 API를 이용해 조회후 DB에 저장하고 주고 (cache miss) (write through)\

}
