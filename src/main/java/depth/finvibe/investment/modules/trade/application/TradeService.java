package depth.finvibe.investment.modules.trade.application;

import depth.finvibe.investment.modules.trade.application.port.in.TradeCommandUseCase;
import depth.finvibe.investment.modules.trade.application.port.out.TradeRepository;
import depth.finvibe.investment.modules.trade.domain.Trade;
import depth.finvibe.investment.modules.trade.domain.enums.TradeType;
import depth.finvibe.investment.modules.trade.domain.error.TradeErrorCode;
import depth.finvibe.investment.modules.trade.dto.TradeDto;
import depth.finvibe.investment.shared.error.DomainException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeService implements TradeCommandUseCase {

    private final TradeRepository tradeRepository;

    @Transactional()
    public TradeDto.TradeResponse getTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new DomainException(TradeErrorCode.TRADE_NOT_FOUND));
        return TradeDto.TradeResponse.from(trade);
    }

    @Transactional
    public TradeDto.TradeResponse createTrade(TradeDto.TransactionRequest request) {

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

        validateCancelTrade(trade);

        trade.cancel();
        Trade cancelledTrade = tradeRepository.save(trade);

        return TradeDto.TradeResponse.from(cancelledTrade);
    }

    // 예약 주문 체결
    @Transactional
    public TradeDto.TradeResponse executeReservedTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new DomainException(TradeErrorCode.TRADE_NOT_FOUND));

        validateReservedTradeForExecution(trade);

        trade.execute();
        Trade saveTrade = tradeRepository.save(trade);

        // TODO: 카프카 이벤트 발행

        return TradeDto.TradeResponse.from(saveTrade);
    }

    private void validateReservedTradeForExecution(Trade trade) {
        if(trade.getTradeType() != TradeType.RESERVED) {
            throw new DomainException(TradeErrorCode.INVALID_TRADE_TYPE);
        }
    }

    private static void validateCancelTrade(Trade trade) {
        if (trade.getTradeType() == TradeType.CANCELLED) {
            throw new DomainException(TradeErrorCode.ALREADY_CANCELLED_TRADE);
        }

        if (trade.getTradeType() != TradeType.RESERVED) {
            throw new DomainException(TradeErrorCode.RESERVED_TRADE_ONLY_CANCELLABLE);
        }
    }

    private TradeDto.TradeResponse processNormalTrade(TradeDto.TransactionRequest request) {
        Trade trade = Trade.create(
                request.getMarketType(),
                request.getStockId(),
                request.getAmount(),
                request.getPrice(),
                request.getPortfolioId(),
                request.getUserId(),
                request.getTransactionType(),
                request.getTradeType()
        );
        Trade savedTrade = tradeRepository.save(trade);

        //TODO: 카프카 이벤트 발행

        return TradeDto.TradeResponse.from(savedTrade);
    }

    private TradeDto.TradeResponse processReservedTrade(TradeDto.TransactionRequest request) {
        Trade trade = Trade.create(
                request.getMarketType(),
                request.getStockId(),
                request.getAmount(),
                request.getPrice(),
                request.getPortfolioId(),
                request.getUserId(),
                request.getTransactionType(),
                request.getTradeType()
        );
        Trade savedTrade = tradeRepository.save(trade);

        return TradeDto.TradeResponse.from(savedTrade);
    }


}
