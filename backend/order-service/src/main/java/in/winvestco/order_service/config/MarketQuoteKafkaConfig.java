package in.winvestco.order_service.config;

import in.winvestco.common.kafka.market.QuoteUpdatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import in.winvestco.order_service.messaging.serialization.ProtobufDeserializer;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ToStringSerializer;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Configuration
public class MarketQuoteKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:order-service-market-quote-consumer}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.topics.market-quote.dlt-name:market.quote.v1.dlt.order-service}")
    private String marketQuoteDltTopicName;

    @Value("${spring.kafka.topics.market-quote.partitions:12}")
    private int marketQuoteTopicPartitions;

    @Value("${spring.kafka.topics.market-quote.replicas:1}")
    private short marketQuoteTopicReplicas;

    @Value("${trading.kafka.market-quote.retry.max-attempts:4}")
    private int retryMaxAttempts;

    @Value("${trading.kafka.market-quote.retry.initial-interval-ms:500}")
    private long retryInitialIntervalMs;

    @Value("${trading.kafka.market-quote.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${trading.kafka.market-quote.retry.max-interval-ms:5000}")
    private long retryMaxIntervalMs;

    @Value("${trading.kafka.market-quote.startup-replay-offset:committed}")
    private String startupReplayOffset;

    @Bean
    public ConsumerFactory<String, QuoteUpdatedEvent> marketQuoteConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ProtobufDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        DefaultKafkaConsumerFactory<String, QuoteUpdatedEvent> factory = new DefaultKafkaConsumerFactory<>(config);
        factory.setValueDeserializer(new ProtobufDeserializer<>(QuoteUpdatedEvent.getDefaultInstance().getParserForType()));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QuoteUpdatedEvent> marketQuoteKafkaListenerContainerFactory(
            DefaultErrorHandler marketQuoteDefaultErrorHandler,
            ConsumerAwareRebalanceListener startupReplayRebalanceListener) {
        ConcurrentKafkaListenerContainerFactory<String, QuoteUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(marketQuoteConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(marketQuoteDefaultErrorHandler);
        factory.getContainerProperties().setConsumerRebalanceListener(startupReplayRebalanceListener);
        return factory;
    }

    @Bean
    public ProducerFactory<String, Object> marketQuoteDltProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ToStringSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> marketQuoteDltKafkaTemplate() {
        return new KafkaTemplate<>(marketQuoteDltProducerFactory());
    }

    @Bean
    public DeadLetterPublishingRecoverer marketQuoteDeadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> marketQuoteDltKafkaTemplate,
            MeterRegistry meterRegistry) {
        return new DeadLetterPublishingRecoverer(
                marketQuoteDltKafkaTemplate,
                (record, ex) -> new TopicPartition(marketQuoteDltTopicName, record.partition())) {
            @Override
            public void accept(org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, Exception exception) {
                Counter.builder("market_quote_dlt_total")
                        .description("Total market quote events sent to DLT")
                        .tag("consumer_group", groupId)
                        .tag("dlt_topic", marketQuoteDltTopicName)
                        .register(meterRegistry)
                        .increment();
                super.accept(record, exception);
            }
        };
    }

    @Bean
    public DefaultErrorHandler marketQuoteDefaultErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(Math.max(retryMaxAttempts - 1, 0));
        backOff.setInitialInterval(retryInitialIntervalMs);
        backOff.setMultiplier(retryMultiplier);
        backOff.setMaxInterval(retryMaxIntervalMs);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                DeserializationException.class,
                org.apache.kafka.common.errors.SerializationException.class,
                MessageConversionException.class,
                ConversionException.class,
                MethodArgumentResolutionException.class);
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }

    @Bean
    public ConsumerAwareRebalanceListener startupReplayRebalanceListener() {
        return new ConsumerAwareRebalanceListener() {
            @Override
            public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
                String replayMode = startupReplayOffset == null
                        ? "committed"
                        : startupReplayOffset.trim().toLowerCase(Locale.ROOT);

                if ("earliest".equals(replayMode)) {
                    consumer.seekToBeginning(partitions);
                } else if ("latest".equals(replayMode)) {
                    consumer.seekToEnd(partitions);
                }
            }
        };
    }

    @Bean
    public NewTopic marketQuoteDltTopic() {
        return TopicBuilder.name(marketQuoteDltTopicName)
                .partitions(marketQuoteTopicPartitions)
                .replicas(marketQuoteTopicReplicas)
                .build();
    }
}
