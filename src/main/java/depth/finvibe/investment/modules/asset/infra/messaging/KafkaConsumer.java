package depth.finvibe.investment.modules.asset.infra.messaging;

import depth.finvibe.investment.modules.asset.application.AssetEventService;
import depth.finvibe.investment.modules.asset.dto.FirstLoginedEvent;
import depth.finvibe.investment.modules.asset.dto.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaConsumer {
    private final AssetEventService assetEventService;

    @KafkaListener(topics = "trade.trade-executed.v1", groupId = "asset-group")
    public void consumeTradeExecutedEvent(ConsumerRecord<String, TradeExecutedEvent> record) {
        TradeExecutedEvent event = record.value();
        assetEventService.handleTradeExecutedEvent(event);
    }

    @KafkaListener(topics = "user.first-logined.v1", groupId = "asset-group")
    public void consumeFirstLoginedEvent(ConsumerRecord<String, FirstLoginedEvent> record) {
        FirstLoginedEvent event = record.value();
        assetEventService.handleFirstLoginedEvent(event);
    }
}