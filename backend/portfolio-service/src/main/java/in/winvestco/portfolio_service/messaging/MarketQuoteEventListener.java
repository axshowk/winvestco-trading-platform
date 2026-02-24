package in.winvestco.portfolio_service.messaging;

import in.winvestco.common.kafka.market.QuoteUpdatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import in.winvestco.portfolio_service.service.MarketQuoteProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketQuoteEventListener {

    private final MarketQuoteProjectionService marketQuoteProjectionService;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "${spring.kafka.topics.market-quote.name:market.quote.v1}",
            groupId = "${spring.kafka.consumer.group-id:portfolio-service-market-quote-consumer}",
            containerFactory = "marketQuoteKafkaListenerContainerFactory")
    public void onQuoteEvent(
            @Payload QuoteUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment,
            Consumer<?, ?> consumer) {
        try {
            marketQuoteProjectionService.upsertQuote(event);
            meterRegistry.counter("market_quote_consumed_total", "service", "portfolio-service", "result", "success")
                    .increment();
            recordConsumerLag(consumer, topic, partition);
            acknowledgment.acknowledge();
            log.debug("Stored market quote projection for symbol {} from partition {} offset {}",
                    event.getSymbol(), partition, offset);
        } catch (Exception e) {
            meterRegistry.counter("market_quote_consumed_total", "service", "portfolio-service", "result", "failure")
                    .increment();
            log.error("Failed to process market quote event for symbol {} at partition {} offset {}",
                    event.getSymbol(), partition, offset, e);
            throw e;
        }
    }

    private void recordConsumerLag(Consumer<?, ?> consumer, String topic, int partition) {
        try {
            TopicPartition topicPartition = new TopicPartition(topic, partition);
            long position = consumer.position(topicPartition);
            Long endOffset = consumer.endOffsets(Collections.singleton(topicPartition)).get(topicPartition);
            if (endOffset != null) {
                long lag = Math.max(0, endOffset - position);
                meterRegistry.summary("market_quote_consumer_lag_records", "service", "portfolio-service")
                        .record(lag);
            }
        } catch (Exception e) {
            log.debug("Unable to record consumer lag for topic {} partition {}", topic, partition, e);
        }
    }
}
