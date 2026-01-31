package depth.finvibe.investment.modules.trade.application;

import depth.finvibe.investment.modules.trade.application.port.in.TradeCommandUseCase;
import depth.finvibe.investment.modules.trade.application.port.in.TradeQueryUseCase;
import depth.finvibe.investment.modules.trade.application.port.out.TradeEventProducer;
import depth.finvibe.investment.modules.trade.application.port.out.TradeRepository;
import depth.finvibe.investment.modules.trade.domain.Trade;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import depth.finvibe.investment.modules.trade.domain.error.TradeErrorCode;
import depth.finvibe.investment.modules.trade.dto.TradeDto;
import depth.finvibe.investment.shared.error.DomainException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeService implements TradeCommandUseCase, TradeQueryUseCase {

    private final TradeRepository tradeRepository;
    private final TradeEventProducer tradeEventProducer;

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
    public TradeDto.TradeResponse createTrade(TradeDto.TransactionRequest request) {

        //TODO: 포트폴리오가 존재하는지, 요청자의 포트폴리오와 일치하는지 검증
        //TODO: 장이 열려있는지 검증
        //TODO: 잔고가 충분한지 검증.

        if (request.getTradeType() == TradeType.NORMAL) {
            return processNormalTrade(request);
        } else if (request.getTradeType() == TradeType.RESERVED) {
            return processReservedTrade(request);
        }

        throw new DomainException(TradeErrorCode.INVALID_TRADE_TYPE);
    }

    @Transactional
    public TradeDto.TradeResponse cancelTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new DomainException(TradeErrorCode.TRADE_NOT_FOUND));

        ensureTradeCancelable(trade);

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

    private static void ensureTradeCancelable(Trade trade) {
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

        tradeEventProducer.publishNormalTradeExecutedEvent(trade);

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

    private TradeDto.TradeResponse processReservedTrade(TradeDto.TransactionRequest request) {
        Trade trade = createTradeFrom(request);
        Trade savedTrade = tradeRepository.save(trade);

        return TradeDto.TradeResponse.from(savedTrade);
    }


}
