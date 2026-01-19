package depth.finvibe.investment.modules.market.application;

import depth.finvibe.investment.modules.market.application.port.in.StockCommandUseCase;
import depth.finvibe.investment.modules.market.application.port.out.CategoryRepository;
import depth.finvibe.investment.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.investment.modules.market.application.port.out.StockRankingRepository;
import depth.finvibe.investment.modules.market.application.port.out.StockRepository;
import depth.finvibe.investment.modules.market.domain.Category;
import depth.finvibe.investment.modules.market.domain.Stock;
import depth.finvibe.investment.modules.market.domain.StockRanking;
import depth.finvibe.investment.modules.market.dto.StockDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StockService implements StockCommandUseCase {

    private final StockRepository stockRepository;
    private final CategoryRepository categoryRepository;
    private final RealMarketClient realMarketClient;
    private final StockRankingRepository stockRankingRepository;

    private static final String FALLBACK_CATEGORY_CODE = "0000";

    @Override
    @Transactional
    public void bulkUpsertStocks() {
        List<StockDto.RealMarketResponse> stocksInKOSPI = realMarketClient.fetchStocksInKOSPI();

        List<Stock> stocksToUpsert = convertToStocksFrom(stocksInKOSPI);

        stockRepository.bulkUpsertStocks(stocksToUpsert);
    }

    private List<Stock> convertToStocksFrom(List<StockDto.RealMarketResponse> stocksInKOSPI) {
        List<Category> allCategories = categoryRepository.findAll();

        return stocksInKOSPI.stream()
                .map(res -> {
                    Category category = seekMatchingCategoryBySymbol(allCategories, res.getSymbol());
                    return createStockFrom(res, category);
                })
                .toList();
    }

    private Stock createStockFrom(StockDto.RealMarketResponse res, Category category) {
        return Stock.builder()
                .symbol(res.getSymbol())
                .name(res.getName())
                .categoryId(category.getId())
                .build();
    }

    private Category seekMatchingCategoryBySymbol(List<Category> allCategories, String symbol) {
        return allCategories.stream()
                .filter(category -> symbol.startsWith(category.getCode()))
                .findFirst()
                .orElseGet(() -> allCategories.stream()
                        .filter(category -> category.getCode().equals(FALLBACK_CATEGORY_CODE))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Fallback category not found")));
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

        List<StockRanking> stockRankings = rankingResponses.stream()
                .filter(ranking -> symbolToStockIdMap.containsKey(ranking.getSymbol()))
                .map(ranking -> createStockRankingFrom(ranking, symbolToStockIdMap.get(ranking.getSymbol())))
                .toList();

        stockRankingRepository.bulkUpsertStockRankings(stockRankings);
    }

    private StockRanking createStockRankingFrom(StockDto.RankingResponse rankingResponse, Long stockId) {
        return StockRanking.builder()
                .stockId(stockId)
                .rankType(rankingResponse.getRankType())
                .rank(rankingResponse.getRank())
                .build();
    }

}

