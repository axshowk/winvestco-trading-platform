package in.winvestco.marketservice.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.kafka.market.IndexSnapshotEvent;
import in.winvestco.common.kafka.market.QuoteUpdatedEvent;
import in.winvestco.marketservice.dto.MarketDataDTO;
import in.winvestco.marketservice.messaging.mapper.MarketKafkaEventMapper;
import in.winvestco.marketservice.messaging.mapper.MarketDataProtobufMapper;
import in.winvestco.marketservice.proto.MarketDataEvent;
import in.winvestco.marketservice.proto.StockData;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final KafkaTemplate<String, MarketDataEvent> legacyKafkaTemplate;
    private final KafkaTemplate<String, QuoteUpdatedEvent> quoteKafkaTemplate;
    private final KafkaTemplate<String, IndexSnapshotEvent> indexKafkaTemplate;
    private final MarketDataProtobufMapper protobufMapper;
    private final MarketKafkaEventMapper marketKafkaEventMapper;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${spring.kafka.topics.market-quote.name:market.quote.v1}")
    private String quoteTopicName;

    @Value("${spring.kafka.topics.market-index.name:market.index.v1}")
    private String indexTopicName;

    private static final String LEGACY_TOPIC_NAME = "market.data.updates";

    /**
     * Publish market data from DTO to Kafka as Protobuf message.
     */
    public void publishMarketData(MarketDataDTO marketDataDTO) {
        if (marketDataDTO == null) {
            log.warn("Cannot publish null market data");
            return;
        }

        MarketDataEvent event = protobufMapper.toProtobuf(marketDataDTO);
        publishLegacyEvent(event, marketDataDTO.getSymbol());

        QuoteUpdatedEvent quoteEvent = marketKafkaEventMapper.toQuoteEvent(event);
        publishQuoteEvent(quoteEvent);
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
                publishLegacyEvent(event, indexName);
                publishIndexEvent(marketKafkaEventMapper.toIndexSnapshotEvent(event));
                publishConstituentQuoteEvents(event);
            } else {
                log.warn("Failed to parse market data for index: {}", indexName);
            }
        } catch (Exception e) {
            log.error("Failed to parse and publish market data for index: {}", indexName, e);
        }
    }

    /**
     * Publish a pre-built legacy Protobuf event to Kafka.
     */
    private void publishLegacyEvent(MarketDataEvent event, String key) {
        if (event == null) {
            return;
        }
        log.info("Publishing legacy market data event to Kafka topic: {}, symbol: {}, constituents: {}",
                LEGACY_TOPIC_NAME, event.getSymbol(), event.getConstituentsCount());

        CompletableFuture<SendResult<String, MarketDataEvent>> future =
                legacyKafkaTemplate.send(LEGACY_TOPIC_NAME, key, event);
        long startNanos = System.nanoTime();

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                recordProducerEvent(LEGACY_TOPIC_NAME, "success", startNanos);
                log.info("Legacy market data event published successfully to topic: {}, partition: {}, offset: {}, symbol: {}",
                        LEGACY_TOPIC_NAME,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getSymbol());
            } else {
                recordProducerEvent(LEGACY_TOPIC_NAME, "failure", startNanos);
                log.error("Failed to publish legacy market data event to Kafka topic: {}, symbol: {}, error: {}",
                        LEGACY_TOPIC_NAME, event.getSymbol(), ex.getMessage(), ex);
            }
        });
    }

    private void publishConstituentQuoteEvents(MarketDataEvent event) {
        if (event.getConstituentsCount() == 0) {
            publishQuoteEvent(marketKafkaEventMapper.toQuoteEvent(event));
            return;
        }

        for (StockData stockData : event.getConstituentsList()) {
            publishQuoteEvent(marketKafkaEventMapper.toQuoteEvent(stockData));
        }
    }

    private void publishQuoteEvent(QuoteUpdatedEvent quoteEvent) {
        if (quoteEvent == null) {
            return;
        }

        CompletableFuture<SendResult<String, QuoteUpdatedEvent>> future =
                quoteKafkaTemplate.send(quoteTopicName, quoteEvent.getSymbol(), quoteEvent);
        long startNanos = System.nanoTime();

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                recordProducerEvent(quoteTopicName, "success", startNanos);
                log.debug("Quote event published to topic: {}, partition: {}, offset: {}, symbol: {}",
                        quoteTopicName,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        quoteEvent.getSymbol());
            } else {
                recordProducerEvent(quoteTopicName, "failure", startNanos);
                log.error("Failed to publish quote event to topic: {}, symbol: {}, error: {}",
                        quoteTopicName, quoteEvent.getSymbol(), ex.getMessage(), ex);
            }
        });
    }

    private void publishIndexEvent(IndexSnapshotEvent indexEvent) {
        if (indexEvent == null) {
            return;
        }

        CompletableFuture<SendResult<String, IndexSnapshotEvent>> future =
                indexKafkaTemplate.send(indexTopicName, indexEvent.getIndexSymbol(), indexEvent);
        long startNanos = System.nanoTime();

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                recordProducerEvent(indexTopicName, "success", startNanos);
                log.debug("Index snapshot event published to topic: {}, partition: {}, offset: {}, index: {}",
                        indexTopicName,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        indexEvent.getIndexSymbol());
            } else {
                recordProducerEvent(indexTopicName, "failure", startNanos);
                log.error("Failed to publish index snapshot event to topic: {}, index: {}, error: {}",
                        indexTopicName, indexEvent.getIndexSymbol(), ex.getMessage(), ex);
            }
        });
    }

    private void recordProducerEvent(String topic, String result, long startNanos) {
        meterRegistry.counter("market_data_producer_events_total", "topic", topic, "result", result).increment();
        Timer.builder("market_data_producer_send_latency_seconds")
                .description("Kafka producer send completion latency")
                .tag("topic", topic)
                .tag("result", result)
                .register(meterRegistry)
                .record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }
}
