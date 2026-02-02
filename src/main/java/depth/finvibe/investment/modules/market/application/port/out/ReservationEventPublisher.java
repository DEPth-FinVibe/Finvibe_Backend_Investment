package depth.finvibe.investment.modules.market.application.port.out;

import depth.finvibe.investment.modules.market.domain.enums.ReservationType;

public interface ReservationEventPublisher {
    void publishReservationConditionMetEvent(Long tradeId, ReservationType type, Long stockId, Long price);
}
