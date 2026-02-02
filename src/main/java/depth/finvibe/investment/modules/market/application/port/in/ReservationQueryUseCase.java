package depth.finvibe.investment.modules.market.application.port.in;

import depth.finvibe.investment.shared.dto.TradeExecutedEvent;

public interface ReservationQueryUseCase {
    void makeReservation(TradeExecutedEvent event);
    void cancelReservation(Long tradeId);
    void reservedStockPriceChanged(Long stockId, Long newPrice);
}
