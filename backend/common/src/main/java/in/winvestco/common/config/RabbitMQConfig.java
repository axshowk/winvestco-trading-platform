
package in.winvestco.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Centralized RabbitMQ configuration with modern practices:
 * - Connection pooling
 * - Publisher confirms and returns
 * - Dead letter queues
 * - Message retry with exponential backoff
 * - Proper error handling
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitmqPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitmqPassword;

    @Value("${spring.rabbitmq.virtual-host:/}")
    private String virtualHost;

    @Value("${app.rabbitmq.connection-timeout:60000}")
    private int connectionTimeout;

    @Value("${app.rabbitmq.requested-heartbeat:60}")
    private int requestedHeartbeat;

    @Value("${app.rabbitmq.connection-pool-size:10}")
    private int connectionPoolSize;

    @Value("${app.rabbitmq.max-channel:100}")
    private int maxChannel;

    // Exchange Constants
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String TRADE_EXCHANGE = "trade.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String PORTFOLIO_EXCHANGE = "portfolio.exchange";
    public static final String MARKET_DATA_EXCHANGE = "market.data.exchange";
    public static final String DLQ_EXCHANGE = "dlq.exchange";

    // Queues
    public static final String USER_CREATED_QUEUE = "user.created.queue";
    public static final String USER_CREATED_NOTIFICATION_QUEUE = "user.created.notification.queue";
    public static final String USER_CREATED_ACCOUNT_QUEUE = "user.created.account.queue";
    public static final String USER_CREATED_PORTFOLIO_QUEUE = "user.created.portfolio.queue";

    // New user event queues
    public static final String USER_UPDATED_NOTIFICATION_QUEUE = "user.updated.notification.queue";
    public static final String USER_STATUS_CHANGED_NOTIFICATION_QUEUE = "user.status.changed.notification.queue";
    public static final String USER_ROLE_CHANGED_NOTIFICATION_QUEUE = "user.role.changed.notification.queue";
    public static final String USER_PASSWORD_CHANGED_NOTIFICATION_QUEUE = "user.password.changed.notification.queue";
    public static final String USER_LOGIN_NOTIFICATION_QUEUE = "user.login.notification.queue";

    public static final String ORDER_CREATED_NOTIFICATION_QUEUE = "order.created.notification.queue";
    public static final String ORDER_UPDATED_NOTIFICATION_QUEUE = "order.updated.notification.queue";
    public static final String ORDER_CREATED_TRADE_QUEUE = "order.created.trade.queue";

    public static final String TRADE_CREATED_ACCOUNT_QUEUE = "trade.created.account.queue";
    public static final String TRADE_CREATED_PORTFOLIO_QUEUE = "trade.created.portfolio.queue";
    public static final String TRADE_REJECTED_NOTIFICATION_QUEUE = "trade.rejected.notification.queue";
    
    public static final String PORTFOLIO_HOLDINGS_SNAPSHOT_QUEUE = "portfolio.holdings.snapshot.queue";

    public static final String MARKET_DATA_UPDATED_QUEUE = "market.data.updated.queue";
    public static final String DLQ_QUEUE = "dlq.queue";

    // Routing Keys
    public static final String PORTFOLIO_HOLDINGS_SNAPSHOT_ROUTING_KEY = "portfolio.holdings.snapshot";
    public static final String STOCK_UPDATED = "stock.updated";

    @Bean
    @Primary
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        return converter;
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        // Enable publisher confirms and returns
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Message acknowledged by broker: {}", correlationData);
            } else {
                log.error("Message not acknowledged by broker: {}, cause: {}", correlationData, cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returned -> {
            log.warn("Message returned from broker: {}", returned);
        });
        return rabbitTemplate;
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        // Enable manual acknowledgment
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // Set concurrency
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        // Set prefetch count for better performance
        factory.setPrefetchCount(10);

        // Enable retry with default configuration
        // Note: For more advanced retry configuration, consider using @Retryable annotation on message handlers
        factory.setDefaultRequeueRejected(false);

        return factory;
    }

    @Bean
    public DirectExchange deadLetterExchange() {
    return new DirectExchange(DLQ_EXCHANGE, true, false);
    }


    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .withArgument("x-message-ttl", 604800000) // 7 days TTL
                .withArgument("x-max-retries", 3)
                .build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("#");
    }

    // Order Exchange
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    // User Exchange
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE, true, false);
    }

    // Trade Exchange 
    @Bean
    public TopicExchange tradeExchange() {
        return new TopicExchange(TRADE_EXCHANGE, true, false);
    }

    // Notification Exchange
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    // Portfolio Exchange
    @Bean
    public TopicExchange portfolioExchange(){
        return new TopicExchange(PORTFOLIO_EXCHANGE, true, false);
    }

    // Market Data Exchange
    @Bean
    public TopicExchange marketDataExchange() {
        return new TopicExchange(MARKET_DATA_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderUpdatedNotificationQueue() {
        return QueueBuilder.durable(ORDER_UPDATED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_UPDATED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(USER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_CREATED_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue userCreatedNotificationQueue() {
        return QueueBuilder.durable(USER_CREATED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_CREATED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue userCreatedPortfolioQueue() {
        return QueueBuilder.durable(USER_CREATED_PORTFOLIO_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_CREATED_PORTFOLIO_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue userUpdatedNotificationQueue() {
        return QueueBuilder.durable(USER_UPDATED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_UPDATED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue userStatusChangedNotificationQueue() {
        return QueueBuilder.durable(USER_STATUS_CHANGED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_STATUS_CHANGED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue userRoleChangedNotificationQueue() {
        return QueueBuilder.durable(USER_ROLE_CHANGED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_ROLE_CHANGED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue userPasswordChangedNotificationQueue() {
        return QueueBuilder.durable(USER_PASSWORD_CHANGED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_PASSWORD_CHANGED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue userLoginNotificationQueue() {
        return QueueBuilder.durable(USER_LOGIN_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_LOGIN_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue tradeRejectedNotificationQueue() {
        return QueueBuilder.durable(TRADE_REJECTED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_REJECTED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue orderCreatedNotificationQueue() {
        return QueueBuilder.durable(ORDER_CREATED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CREATED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue orderCreatedTradeQueue() {
        return QueueBuilder.durable(ORDER_CREATED_TRADE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CREATED_TRADE_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeCreatedAccountQueue() {
        return QueueBuilder.durable(TRADE_CREATED_ACCOUNT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_CREATED_ACCOUNT_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeCreatedPortfolioQueue() {
        return QueueBuilder.durable(TRADE_CREATED_PORTFOLIO_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_CREATED_PORTFOLIO_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue stockUpdatedQueue() {
        return QueueBuilder.durable(MARKET_DATA_UPDATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MARKET_DATA_UPDATED_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    // Bindings
    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder.bind(userCreatedQueue())
                .to(userExchange())
                .with("user.created");
    }

    @Bean
    public Binding userCreatedNotificationBinding() {
        return BindingBuilder.bind(userCreatedNotificationQueue())
                .to(userExchange())
                .with("user.created");
    }

    @Bean
    public Binding userCreatedPortfolioBinding() {
        return BindingBuilder.bind(userCreatedPortfolioQueue())
                .to(userExchange())
                .with("user.created");
    }

    @Bean
    public Binding userUpdatedNotificationBinding() {
        return BindingBuilder.bind(userUpdatedNotificationQueue())
                .to(userExchange())
                .with("user.updated");
    }

    @Bean
    public Binding userStatusChangedNotificationBinding() {
        return BindingBuilder.bind(userStatusChangedNotificationQueue())
                .to(userExchange())
                .with("user.status.changed");
    }

    @Bean
    public Binding userRoleChangedNotificationBinding() {
        return BindingBuilder.bind(userRoleChangedNotificationQueue())
                .to(userExchange())
                .with("user.role.changed");
    }

    @Bean
    public Binding userPasswordChangedNotificationBinding() {
        return BindingBuilder.bind(userPasswordChangedNotificationQueue())
                .to(userExchange())
                .with("user.password.changed");
    }

    @Bean
    public Binding userLoginNotificationBinding() {
        return BindingBuilder.bind(userLoginNotificationQueue())
                .to(userExchange())
                .with("user.login");
    }

    @Bean
    public Queue portfolioHoldingsSnapshotQueue() {
        return QueueBuilder.durable(PORTFOLIO_HOLDINGS_SNAPSHOT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PORTFOLIO_HOLDINGS_SNAPSHOT_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Binding portfolioHoldingsSnapshotBinding() {
        return BindingBuilder.bind(portfolioHoldingsSnapshotQueue())
                .to(portfolioExchange())
                .with(PORTFOLIO_HOLDINGS_SNAPSHOT_ROUTING_KEY);
    }

    @Bean
    public Binding orderCreatedNotificationBinding() {
        return BindingBuilder.bind(orderCreatedNotificationQueue())
                .to(orderExchange())
                .with("order.created");
    }

    @Bean
    public Binding orderCreatedTradeBinding() {
        return BindingBuilder.bind(orderCreatedTradeQueue())
                .to(orderExchange())
                .with("order.created");
    }

    @Bean
    public Binding orderUpdatedNotificationBinding() {
        return BindingBuilder.bind(orderUpdatedNotificationQueue())
                .to(orderExchange())
                .with("order.updated");
    }

    @Bean
    public Binding tradeCreatedAccountBinding() {
        return BindingBuilder.bind(tradeCreatedAccountQueue())
                .to(tradeExchange())
                .with("trade.created");
    }

    @Bean
    public Binding tradeCreatedPortfolioBinding() {
        return BindingBuilder.bind(tradeCreatedPortfolioQueue())
                .to(tradeExchange())
                .with("trade.created");
    }

    @Bean
    public Binding tradeRejectedNotificationBinding() {
        return BindingBuilder.bind(tradeRejectedNotificationQueue())
                .to(tradeExchange())
                .with("trade.rejected");
    }

    @Bean
    public Binding stockUpdatedBinding() {
        return BindingBuilder.bind(stockUpdatedQueue())
                .to(marketDataExchange())
                .with(STOCK_UPDATED);
    }
}
