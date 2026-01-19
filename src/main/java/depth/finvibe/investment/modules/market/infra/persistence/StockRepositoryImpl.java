package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Stock;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepository {

    private final StockJpaRepository jpaRepository;

    @Override
    public Optional<Stock> findById(Long stockId) {
        return jpaRepository.findById(stockId);
    }

    @Override
    public Optional<Stock> findBySymbol(String symbol) {
        return jpaRepository.findBySymbol(symbol);
    }

    @Override
    public void save(Stock stock) {
        jpaRepository.save(stock);
    }

    @Override
    public boolean existsById(Long stockId) {
        return jpaRepository.existsById(stockId);
    }

    @Override
    @Transactional
    public void bulkUpsertStocks(List<Stock> stocksToUpsert) {
        jpaRepository.saveAll(stocksToUpsert);
    }

    @Override
    public List<Stock> findAllBySymbolIn(List<String> symbols) {
        return jpaRepository.findAllBySymbolIn(symbols);
    }

    @Override
    public List<Stock> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Stock> findAllById(List<Long> stockIds) {
        return jpaRepository.findAllById(stockIds);
    }
}