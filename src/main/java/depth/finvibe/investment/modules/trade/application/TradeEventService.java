package depth.finvibe.investment.modules.trade.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.investment.modules.trade.application.port.in.TradeCommandUseCase;
import depth.finvibe.investment.modules.trade.application.port.in.TradeEventUseCase;
import depth.finvibe.investment.modules.trade.domain.error.TradeErrorCode;
import depth.finvibe.investment.modules.trade.dto.TradeDto;
import depth.finvibe.investment.shared.dto.ReservationSatisfiedEvent;
import depth.finvibe.investment.shared.error.DomainException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeEventService implements TradeEventUseCase {

    private final TradeCommandUseCase tradeCommandUseCase;

    @Transactional
    public void processReservedTradeExecution(ReservationSatisfiedEvent event) {
        log.info("예약 거래 조건 충족 처리 시작: tradeId={}, userId={}, type={}, amount={}, price={}",
            event.tradeId(), event.userId(), event.type(), event.amount(), event.price());

        try {
            Long tradeId = Long.parseLong(event.tradeId());

            TradeDto.TradeResponse response = tradeCommandUseCase.executeReservedTrade(tradeId);

            log.info("예약 거래 체결 완료: tradeId={}, amount={}, price={}",
                response.getTradeId(),
                response.getAmount(),
                response.getPrice());

        } catch (NumberFormatException e) {
            log.error("잘못된 tradeId 형식: tradeId={}", event.tradeId(), e);
            throw new DomainException(TradeErrorCode.INVALID_TRADE_ID_FORMAT);
        } catch (Exception e) {
            log.error("예약 거래 체결 실패: tradeId={}, userId={}, error={}",
                event.tradeId(), event.userId(), e.getMessage(), e);
            throw e;
        }
    }
}
