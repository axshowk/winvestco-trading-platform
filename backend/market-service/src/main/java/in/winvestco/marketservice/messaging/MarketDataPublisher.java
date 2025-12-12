package in.winvestco.marketservice.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_NAME = "market.data.updates";

    public void publishMarketData(String marketData) {
        log.info("Publishing market data to Kafka topic: {}", TOPIC_NAME);
        
        // Send message asynchronously
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(TOPIC_NAME, "market-update", marketData);
        
        // Add callback for success/failure
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Market data published successfully to topic: {}, partition: {}, offset: {}", 
                    TOPIC_NAME, 
                    result.getRecordMetadata().partition(), 
                    result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish market data to Kafka topic: {}, error: {}", 
                    TOPIC_NAME, ex.getMessage(), ex);
            }
        });
    }
}
