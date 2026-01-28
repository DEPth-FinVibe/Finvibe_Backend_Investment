package depth.finvibe.investment.modules.trade.application.port.in;

import depth.finvibe.investment.shared.dto.ReservationSatisfiedEvent;

public interface TradeEventUseCase {
    void processReservedTradeExecution(ReservationSatisfiedEvent event);
}
