package depth.finvibe.investment.modules.wallet.infra.messaging;

import depth.finvibe.investment.modules.wallet.application.WalletEventService;
import depth.finvibe.investment.shared.dto.FirstLoginedEvent;
import depth.finvibe.investment.shared.dto.TradeExecutedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletKafkaConsumer {
    private final WalletEventService walletEventService;

    @KafkaListener(topics = "trade.trade-executed.v1", groupId = "wallet-group")
    public void consumeTradeExecutedEvent(ConsumerRecord<String, TradeExecutedEvent> record) {
        TradeExecutedEvent event = record.value();
        walletEventService.handleTradeExecutedEvent(event);
    }

    @KafkaListener(topics = "user.first-logined.v1", groupId = "wallet-group")
    public void consumeFirstLoginedEvent(ConsumerRecord<String, FirstLoginedEvent> record) {
        FirstLoginedEvent event = record.value();
        walletEventService.handleFirstLoginedEvent(event);
    }
}
