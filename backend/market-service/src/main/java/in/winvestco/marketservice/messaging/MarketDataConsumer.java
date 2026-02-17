package in.winvestco.marketservice.messaging;

import in.winvestco.marketservice.proto.MarketDataEvent;
import in.winvestco.marketservice.proto.StockData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Consumer for market data events from Kafka.
 * Processes Protobuf MarketDataEvent messages.
 */
@Service
@Slf4j
public class MarketDataConsumer {

    private static final String TOPIC_NAME = "market.data.updates";
    private static final String GROUP_ID = "market-data-consumer-group";

    /**
     * Consume market data events from Kafka.
     * Uses Protobuf deserialization.
     */
    @KafkaListener(
            topics = TOPIC_NAME,
            groupId = GROUP_ID,
            containerFactory = "protobufKafkaListenerContainerFactory"
    )
    public void consumeMarketData(
            @Payload MarketDataEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received market data event from Kafka - Topic: {}, Partition: {}, Offset: {}, Symbol: {}",
                TOPIC_NAME, partition, offset, event.getSymbol());

        try {
            processMarketData(event);
        } catch (Exception e) {
            log.error("Error processing market data event: symbol={}, error={}",
                    event.getSymbol(), e.getMessage(), e);
        }
    }

    private void processMarketData(MarketDataEvent event) {
        log.debug("Processing market data for symbol: {} on exchange: {}",
                event.getSymbol(), event.getExchange());

        // Log index summary
        log.info("Index: {} | LTP: {} | Change: {} ({}%) | Volume: {}",
                event.getSymbol(),
                event.getLastTradedPrice(),
                event.getChangeValue(),
                event.getChangePercentage(),
                event.getVolume());

        // Process constituents if available
        List<StockData> constituents = event.getConstituentsList();
        if (!constituents.isEmpty()) {
            log.info("Processing {} constituent stocks for index: {}",
                    constituents.size(), event.getIndexName());

            for (StockData stock : constituents) {
                log.debug("Stock: {} | Price: {} | Change: {}%",
                        stock.getSymbol(),
                        stock.getLastPrice(),
                        stock.getPercentChange());
            }
        }
    }
}
