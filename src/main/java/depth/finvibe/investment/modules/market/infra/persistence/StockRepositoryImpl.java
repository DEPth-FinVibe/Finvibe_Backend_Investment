package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StockRepositoryImpl implements StockRepository {

    @Override
    public Optional<Stock> findById(Long stockId) {
        return Optional.empty();
    }

    @Override
    public Optional<Stock> findBySymbol(String symbol) {
        return Optional.empty();
    }

    @Override
    public void save(Stock stock) {

    }

    @Override
    public boolean existsById(Long stockId) {
        return false;
    }

    @Override
    public void bulkUpsertStocks(List<Stock> stocksToUpsert) {

    }

    @Override
    public List<Stock> findAllBySymbolIn(List<String> symbols) {
        return List.of();
    }
}
