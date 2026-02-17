package in.winvestco.marketservice.config;

import in.winvestco.marketservice.messaging.serialization.ProtobufDeserializer;
import in.winvestco.marketservice.messaging.serialization.ProtobufSerializer;
import in.winvestco.marketservice.proto.MarketDataEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for Protobuf serialization/deserialization.
 * Configures producer and consumer factories with Protobuf support.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:market-data-consumer-group}")
    private String groupId;

    /**
     * Producer factory for Protobuf MarketDataEvent messages.
     */
    @Bean
    public ProducerFactory<String, MarketDataEvent> protobufProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ProtobufSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * KafkaTemplate for Protobuf MarketDataEvent messages.
     */
    @Bean
    public KafkaTemplate<String, MarketDataEvent> protobufKafkaTemplate() {
        return new KafkaTemplate<>(protobufProducerFactory());
    }

    /**
     * Consumer factory for Protobuf MarketDataEvent messages.
     */
    @Bean
    public ConsumerFactory<String, MarketDataEvent> protobufConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ProtobufDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        DefaultKafkaConsumerFactory<String, MarketDataEvent> factory =
                new DefaultKafkaConsumerFactory<>(config);

        // Set the Protobuf parser for deserialization
        factory.setValueDeserializer(new ProtobufDeserializer<>(MarketDataEvent.getDefaultInstance().getParserForType()));

        return factory;
    }

    /**
     * Listener container factory for Protobuf messages.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MarketDataEvent> protobufKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MarketDataEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(protobufConsumerFactory());
        factory.setConcurrency(3);
        factory.setBatchListener(false);
        return factory;
    }

    /**
     * Legacy producer factory for backward compatibility (if needed).
     * Can be removed once all services are migrated to Protobuf.
     */
    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }
}
