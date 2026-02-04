package depth.finvibe.investment.modules.market.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.investment.modules.market.application.port.in.StockCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.StockRankingRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockThemeRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.StockRanking;
import depth.finvibe.investment.modules.market.domain.enums.RankType;
import depth.finvibe.investment.modules.market.dto.StockDto;

@RequiredArgsConstructor
@Service
public class StockService implements StockCommandUseCase {

    private static final String FALLBACK_CATEGORY_NAME = "기타";

    private final StockRepository stockRepository;
    private final CategoryRepository categoryRepository;
    private final StockThemeRepository stockThemeRepository;
    private final RealMarketClient realMarketClient;
    private final StockRankingRepository stockRankingRepository;

    @Override
    @Transactional
    public void bulkUpsertStocks() {
        List<StockDto.RealMarketStockResponse> stocksInKOSPI = realMarketClient.fetchStocksInRealMarket();

        List<Stock> stocksToUpsert = convertToStocksFrom(stocksInKOSPI);

        stockRepository.bulkUpsertStocks(stocksToUpsert);
    }

    private List<Stock> convertToStocksFrom(List<StockDto.RealMarketStockResponse> stocksInKOSPI) {
        List<Category> allCategories = categoryRepository.findAll();
        Map<String, Category> categoryByName = allCategories.stream()
                .collect(Collectors.toMap(Category::getName, category -> category, (existing, replacement) -> existing));
        Map<String, String> symbolToThemeMap = stockThemeRepository.findSymbolToThemeMap();

        return stocksInKOSPI.stream()
                .map(res -> {
                    String theme = symbolToThemeMap.get(res.getSymbol());
                    Category category = resolveCategory(categoryByName, theme);
                    return createStockFrom(res, category);
                })
                .toList();
    }

    private Stock createStockFrom(StockDto.RealMarketStockResponse res, Category category) {
        return Stock.builder()
                .symbol(res.getSymbol())
                .name(res.getName())
                .marketType(res.getMarketType())
                .categoryId(category.getId())
                .build();
    }

    private Category resolveCategory(Map<String, Category> categoryByName, String theme) {
        if (theme != null && !theme.isBlank()) {
            Category matched = categoryByName.get(theme);
            if (matched != null) {
                return matched;
            }
        }
        Category fallback = categoryByName.get(FALLBACK_CATEGORY_NAME);
        if (fallback == null) {
            throw new IllegalStateException("Fallback category not found");
        }
        return fallback;
    }

    @Override
    @Transactional
    public void renewStockCharts() {
        List<StockDto.RankingResponse> rankingResponses = realMarketClient.fetchStockRankings();

        List<String> symbols = rankingResponses.stream()
                .map(StockDto.RankingResponse::getSymbol)
                .toList();
        List<Stock> stocks = stockRepository.findAllBySymbolIn(symbols);
        Map<String, Long> symbolToStockIdMap = stocks.stream()
                .collect(Collectors.toMap(Stock::getSymbol, Stock::getId));

        List<RankType> rankTypes = rankingResponses.stream()
                .map(StockDto.RankingResponse::getRankType)
                .distinct()
                .toList();
        stockRankingRepository.deleteByRankTypeIn(rankTypes);

        List<StockRanking> stockRankings = rankingResponses.stream()
                .filter(ranking -> symbolToStockIdMap.containsKey(ranking.getSymbol()))
                .map(ranking -> createStockRankingFrom(
                        ranking,
                        symbolToStockIdMap.get(ranking.getSymbol())
                ))
                .toList();

        stockRankingRepository.bulkUpsertStockRankings(stockRankings);
    }

    private StockRanking createStockRankingFrom(StockDto.RankingResponse rankingResponse, Long stockId) {
        return StockRanking.builder()
                .stockId(stockId)
                .rankType(rankingResponse.getRankType())
                .rank(rankingResponse.getRank())
                .updatedAt(LocalDateTime.now())
                .build();
    }

}
