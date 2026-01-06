package depth.finvibe.investment.modules.trade.infra.messaging;

import depth.finvibe.investment.modules.trade.application.TradeEventService;
import depth.finvibe.investment.shared.dto.ReservationSatisfiedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeKafkaConsumer {
    private final TradeEventService tradeEventService;

    @KafkaListener(topics = "market.reservation-satisfied.v1", groupId = "trade-group")
    public void consumeReservationSatisfiedEvent(ConsumerRecord<String, ReservationSatisfiedEvent> record) {
        ReservationSatisfiedEvent event = record.value();
        tradeEventService.processReservedTradeExecution(event);
    }
}
