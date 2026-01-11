package depth.finvibe.investment;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import java.util.regex.Pattern;

import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class FinvibeInvestmentApplicationTests {

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class KafkaListenerTestConfig {
        @Bean(name = "kafkaListenerContainerFactory")
        KafkaListenerContainerFactory<MessageListenerContainer> kafkaListenerContainerFactory() {
            return new KafkaListenerContainerFactory<>() {
                @Override
                public MessageListenerContainer createListenerContainer(KafkaListenerEndpoint endpoint) {
                    return mockContainer();
                }

                @Override
                public MessageListenerContainer createContainer(TopicPartitionOffset... topicPartitions) {
                    return mockContainer();
                }

                @Override
                public MessageListenerContainer createContainer(String... topics) {
                    return mockContainer();
                }

                @Override
                public MessageListenerContainer createContainer(Pattern topicPattern) {
                    return mockContainer();
                }

                private MessageListenerContainer mockContainer() {
                    MessageListenerContainer container = Mockito.mock(MessageListenerContainer.class);
                    Mockito.when(container.getPhase())
                            .thenReturn(AbstractMessageListenerContainer.DEFAULT_PHASE);
                    Mockito.when(container.isAutoStartup()).thenReturn(false);
                    Mockito.when(container.isRunning()).thenReturn(false);
                    return container;
                }
            };
        }
    }
}
