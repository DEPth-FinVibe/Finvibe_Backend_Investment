package depth.finvibe.investment.modules.trade.infra.messaging;

import depth.finvibe.investment.modules.trade.application.TradeEventService;
import depth.finvibe.investment.shared.dto.ReservationSatisfiedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeKafkaConsumer {
    private final TradeEventService tradeEventService;

    @KafkaListener(
            topics = "market.reservation-satisfied.v1",
            groupId = "trade-group",
            properties = {
                "spring.json.value.default.type=depth.finvibe.investment.shared.dto.ReservationSatisfiedEvent"
            }
    )
    public void consumeReservationSatisfiedEvent(ConsumerRecord<String, ReservationSatisfiedEvent> record) {
        log.info("Consumed ReservationSatisfiedEvent from topic: {}, key: {}, offset: {}",
                record.topic(), record.key(), record.offset());
        ReservationSatisfiedEvent event = record.value();
        tradeEventService.processReservedTradeExecution(event);
    }
}
