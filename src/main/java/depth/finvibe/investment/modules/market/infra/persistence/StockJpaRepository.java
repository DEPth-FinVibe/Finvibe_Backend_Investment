package depth.finvibe.investment.modules.market.infra.persistence;

import depth.finvibe.investment.modules.market.domain.Stock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockJpaRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findBySymbol(String symbol);

    List<Stock> findAllBySymbolIn(List<String> symbols);

    List<Stock> findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(String nameQuery, String symbolQuery);
}
