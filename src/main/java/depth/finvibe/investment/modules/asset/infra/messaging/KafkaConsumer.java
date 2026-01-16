package depth.finvibe.investment.modules.asset.infra.messaging;

import depth.finvibe.investment.modules.asset.application.AssetEventService;
import depth.finvibe.investment.shared.dto.SignUpEvent;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaConsumer {
    private final AssetEventService assetEventService;

    @KafkaListener(
        topics = "trade.trade-executed.v1", 
        groupId = "asset-group",
        properties = {
            "spring.json.value.default.type=depth.finvibe.investment.shared.dto.TradeExecutedEvent"
        }
    )
    public void consumeTradeExecutedEvent(ConsumerRecord<String, TradeExecutedEvent> record) {
        TradeExecutedEvent event = record.value();
        assetEventService.handleTradeExecutedEvent(event);
    }

    @KafkaListener(
        topics = "user.signup.v1", 
        groupId = "asset-group",
        properties = {
            "spring.json.value.default.type=depth.finvibe.investment.shared.dto.SignUpEvent"
        }
    )
    public void consumeSignUpEvent(ConsumerRecord<String, SignUpEvent> record) {
        SignUpEvent event = record.value();
        assetEventService.handleSignUpEvent(event);
    }
}