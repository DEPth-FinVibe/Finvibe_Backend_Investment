package depth.finvibe.investment.modules.market.application.port.out;

import java.util.Set;

public interface RegionOfInterestRepository {
    void addToLevel1(Long stockId);
    void addToLevel2(Long stockId);
    void removeFromLevel1(Long stockId);
    void removeFromLevel2(Long stockId);
    Set<Long> getLevel1StockIds();
    Set<Long> getLevel2StockIds();
    Long getLevel1Count(Long stockId);
    Long getLevel2Count(Long stockId);
    boolean existsInLevel1(Long stockId);
    boolean existsInLevel2(Long stockId);
}