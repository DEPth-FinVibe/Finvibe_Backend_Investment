package depth.finvibe.investment.modules.trade.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import depth.finvibe.investment.boot.security.model.Requester;
import depth.finvibe.investment.boot.security.model.UserRole;
import depth.finvibe.investment.modules.trade.application.port.in.TradeCommandUseCase;
import depth.finvibe.investment.modules.trade.application.port.in.TradeQueryUseCase;
import depth.finvibe.investment.modules.trade.application.port.out.AssetClient;
import depth.finvibe.investment.modules.trade.application.port.out.MarketClient;
import depth.finvibe.investment.modules.trade.application.port.out.TradeEventProducer;
import depth.finvibe.investment.modules.trade.application.port.out.TradeRepository;
import depth.finvibe.investment.modules.trade.application.port.out.WalletClient;
import depth.finvibe.investment.modules.trade.domain.Trade;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import depth.finvibe.investment.modules.trade.domain.enums.TransactionType;
import depth.finvibe.investment.modules.trade.domain.error.TradeErrorCode;
import depth.finvibe.investment.modules.trade.dto.TradeDto;
import depth.finvibe.investment.shared.application.port.out.GamificationEventProducer;
import depth.finvibe.investment.shared.dto.MetricEventType;
import depth.finvibe.investment.shared.dto.UserMetricUpdatedEvent;
import depth.finvibe.investment.shared.error.DomainException;
@Service
@RequiredArgsConstructor
public class TradeService implements TradeCommandUseCase, TradeQueryUseCase {

    private final TradeRepository tradeRepository;
    private final TradeEventProducer tradeEventProducer;
    private final GamificationEventProducer gamificationEventProducer;
    private final AssetClient assetClient;
    private final MarketClient marketClient;
    private final WalletClient walletClient;


    @Transactional()
    public TradeDto.TradeResponse findTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new DomainException(TradeErrorCode.TRADE_NOT_FOUND));
        return TradeDto.TradeResponse.from(trade);
    }

    @Transactional
    public List<Long> findReservedStockIds(UUID userId) {
        return tradeRepository.findDistinctStockIdsByUserIdAndTradeType(userId, TradeType.RESERVED);
    }

    @Transactional
    public TradeDto.TradeResponse createTrade(TradeDto.TransactionRequest request, Requester requester) {
        validateTradeContexts(request, requester);

        if (request.getTradeType() == TradeType.NORMAL) {
            return processNormalTrade(request);
        } else if (request.getTradeType() == TradeType.RESERVED) {
            return processReservedTrade(request);
        }

        throw new DomainException(TradeErrorCode.INVALID_TRADE_TYPE);
    }

    private void validateTradeContexts(TradeDto.TransactionRequest request, Requester requester) {
        if(!marketClient.isMarketOpen()) {
            throw new DomainException(TradeErrorCode.MARKET_CLOSED);
        }

        if(!assetClient.isExistPortfolio(request.getPortfolioId(), requester.getUuid())) {
            throw new DomainException(TradeErrorCode.PORTFOLIO_NOT_FOUND);
        }

        Long balance = walletClient.getWalletBalance(requester.getUuid());
        if(balance < request.getAmount() * request.getPrice()) {
            throw new DomainException(TradeErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    @Transactional
    public TradeDto.TradeResponse cancelTrade(Long tradeId, Requester requester) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new DomainException(TradeErrorCode.TRADE_NOT_FOUND));

        ensureTradeCancelable(trade, requester);

        trade.cancel();
        Trade cancelledTrade = tradeRepository.save(trade);

        return TradeDto.TradeResponse.from(cancelledTrade);
    }

    // 예약 주문 체결
    @Transactional
    public TradeDto.TradeResponse executeReservedTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new DomainException(TradeErrorCode.TRADE_NOT_FOUND));

        ensureTradeIsReserved(trade);

        trade.execute();
        Trade saveTrade = tradeRepository.save(trade);

        tradeEventProducer.publishReservedTradeExecutedEvent(trade);

        return TradeDto.TradeResponse.from(saveTrade);
    }

    private void ensureTradeIsReserved(Trade trade) {
        if(trade.getTradeType() != TradeType.RESERVED) {
            throw new DomainException(TradeErrorCode.INVALID_TRADE_TYPE);
        }
    }

    private static void ensureTradeCancelable(Trade trade, Requester requester) {
        if (!trade.getUserId().equals(requester.getUuid()) && requester.getRole() != UserRole.ADMIN) {
            throw new DomainException(TradeErrorCode.CANNOT_CANCEL_BY_OTHER_USER);
        }

        if (trade.getTradeType() == TradeType.CANCELLED) {
            throw new DomainException(TradeErrorCode.ALREADY_CANCELLED_TRADE);
        }

        if (trade.getTradeType() != TradeType.RESERVED) {
            throw new DomainException(TradeErrorCode.RESERVED_TRADE_ONLY_CANCELLABLE);
        }
    }

    private TradeDto.TradeResponse processNormalTrade(TradeDto.TransactionRequest request) {
        Trade trade = createTradeFrom(request);
        Trade savedTrade = tradeRepository.save(trade);

        //TODO: 실제 시장 가격과 다르면 오류 발생

        tradeEventProducer.publishNormalTradeExecutedEvent(trade);
        publishTradeMetricEvent(trade);

        return TradeDto.TradeResponse.from(savedTrade);
    }

    private static Trade createTradeFrom(TradeDto.TransactionRequest request) {
        return Trade.create(
                request.getMarketType(),
                request.getStockId(),
                request.getAmount(),
                request.getPrice(),
                request.getPortfolioId(),
                request.getUserId(),
                request.getTransactionType(),
                request.getTradeType()
        );
    }

    private void publishTradeMetricEvent(Trade trade) {
        MetricEventType eventType = trade.getTransactionType() == TransactionType.BUY
                ? MetricEventType.STOCK_BOUGHT
                : MetricEventType.STOCK_SOLD;

        gamificationEventProducer.publishUserMetricUpdatedEvent(UserMetricUpdatedEvent.builder()
                .userId(trade.getUserId().toString())
                .eventType(eventType)
                .delta(1.0)
                .occurredAt(Instant.now())
                .build());
    }

    private TradeDto.TradeResponse processReservedTrade(TradeDto.TransactionRequest request) {
        Trade trade = createTradeFrom(request);
        Trade savedTrade = tradeRepository.save(trade);

        return TradeDto.TradeResponse.from(savedTrade);
    }


}
