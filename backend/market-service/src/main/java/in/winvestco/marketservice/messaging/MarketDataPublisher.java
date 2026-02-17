package in.winvestco.marketservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.marketservice.dto.MarketDataDTO;
import in.winvestco.marketservice.messaging.mapper.MarketDataProtobufMapper;
import in.winvestco.marketservice.proto.MarketDataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes market data events to Kafka using Protobuf serialization.
 * Replaces the old String-based publisher with structured Protobuf messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataPublisher {

    private final KafkaTemplate<String, MarketDataEvent> kafkaTemplate;
    private final MarketDataProtobufMapper protobufMapper;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_NAME = "market.data.updates";

    /**
     * Publish market data from DTO to Kafka as Protobuf message.
     */
    public void publishMarketData(MarketDataDTO marketDataDTO) {
        if (marketDataDTO == null) {
            log.warn("Cannot publish null market data");
            return;
        }

        MarketDataEvent event = protobufMapper.toProtobuf(marketDataDTO);
        publishEvent(event, marketDataDTO.getSymbol());
    }

    /**
     * Publish full NSE JSON response to Kafka as Protobuf message.
     * Parses the JSON and creates a structured event with constituents.
     */
    public void publishMarketData(String indexName, String jsonData) {
        if (jsonData == null || jsonData.isEmpty()) {
            log.warn("Cannot publish empty market data for index: {}", indexName);
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonData);
            MarketDataEvent event = protobufMapper.fromNseJson(indexName, root);

            if (event != null) {
                publishEvent(event, indexName);
            } else {
                log.warn("Failed to parse market data for index: {}", indexName);
            }
        } catch (Exception e) {
            log.error("Failed to parse and publish market data for index: {}", indexName, e);
        }
    }

    /**
     * Publish a pre-built Protobuf event to Kafka.
     */
    private void publishEvent(MarketDataEvent event, String key) {
        log.info("Publishing market data event to Kafka topic: {}, symbol: {}, constituents: {}",
                TOPIC_NAME, event.getSymbol(), event.getConstituentsCount());

        CompletableFuture<SendResult<String, MarketDataEvent>> future =
                kafkaTemplate.send(TOPIC_NAME, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Market data event published successfully to topic: {}, partition: {}, offset: {}, symbol: {}",
                        TOPIC_NAME,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getSymbol());
            } else {
                log.error("Failed to publish market data event to Kafka topic: {}, symbol: {}, error: {}",
                        TOPIC_NAME, event.getSymbol(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * Get the topic name.
     */
    public String getTopicName() {
        return TOPIC_NAME;
    }
}
