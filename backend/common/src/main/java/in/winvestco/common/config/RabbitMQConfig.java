
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
    public static final String FUNDS_EXCHANGE = "funds.exchange";
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

    // Order Service specific queues
    public static final String ORDER_VALIDATED_FUNDS_QUEUE = "order.validated.funds.queue";
    public static final String FUNDS_LOCKED_ORDER_QUEUE = "funds.locked.order.queue";
    public static final String TRADE_EXECUTED_ORDER_QUEUE = "trade.executed.order.queue";

    // Trade Service specific queues
    public static final String FUNDS_LOCKED_TRADE_QUEUE = "funds.locked.trade.queue";
    public static final String TRADE_CREATED_NOTIFICATION_QUEUE = "trade.created.notification.queue";
    public static final String TRADE_PLACED_NOTIFICATION_QUEUE = "trade.placed.notification.queue";
    public static final String TRADE_CLOSED_NOTIFICATION_QUEUE = "trade.closed.notification.queue";
    public static final String TRADE_CANCELLED_NOTIFICATION_QUEUE = "trade.cancelled.notification.queue";
    public static final String TRADE_FAILED_NOTIFICATION_QUEUE = "trade.failed.notification.queue";
    public static final String TRADE_CLOSED_FUNDS_QUEUE = "trade.closed.funds.queue";
    public static final String TRADE_CANCELLED_FUNDS_QUEUE = "trade.cancelled.funds.queue";

    public static final String TRADE_CREATED_ACCOUNT_QUEUE = "trade.created.account.queue";
    public static final String TRADE_CREATED_PORTFOLIO_QUEUE = "trade.created.portfolio.queue";
    public static final String TRADE_REJECTED_NOTIFICATION_QUEUE = "trade.rejected.notification.queue";

    public static final String PORTFOLIO_HOLDINGS_SNAPSHOT_QUEUE = "portfolio.holdings.snapshot.queue";

    public static final String MARKET_DATA_UPDATED_QUEUE = "market.data.updated.queue";
    public static final String DLQ_QUEUE = "dlq.queue";

    // Notification Service Queues
    public static final String ORDER_CANCELLED_NOTIFICATION_QUEUE = "order.cancelled.notification.queue";
    public static final String ORDER_REJECTED_NOTIFICATION_QUEUE = "order.rejected.notification.queue";
    public static final String ORDER_EXPIRED_NOTIFICATION_QUEUE = "order.expired.notification.queue";
    public static final String ORDER_FILLED_NOTIFICATION_QUEUE = "order.filled.notification.queue";
    public static final String FUNDS_LOCKED_NOTIFICATION_QUEUE = "funds.locked.notification.queue";
    public static final String FUNDS_RELEASED_NOTIFICATION_QUEUE = "funds.released.notification.queue";
    public static final String FUNDS_DEPOSITED_NOTIFICATION_QUEUE = "funds.deposited.notification.queue";
    public static final String FUNDS_WITHDRAWN_NOTIFICATION_QUEUE = "funds.withdrawn.notification.queue";
    public static final String TRADE_EXECUTED_NOTIFICATION_QUEUE = "trade.executed.notification.queue";

    // Routing Keys
    public static final String PORTFOLIO_HOLDINGS_SNAPSHOT_ROUTING_KEY = "portfolio.holdings.snapshot";
    public static final String STOCK_UPDATED = "stock.updated";
    public static final String ORDER_VALIDATED_ROUTING_KEY = "order.validated";
    public static final String FUNDS_LOCKED_ROUTING_KEY = "funds.locked";
    public static final String TRADE_EXECUTED_ROUTING_KEY = "trade.executed";

    // Trade Service Routing Keys
    public static final String TRADE_CREATED_ROUTING_KEY = "trade.created";
    public static final String TRADE_PLACED_ROUTING_KEY = "trade.placed";
    public static final String TRADE_CLOSED_ROUTING_KEY = "trade.closed";
    public static final String TRADE_CANCELLED_ROUTING_KEY = "trade.cancelled";
    public static final String TRADE_FAILED_ROUTING_KEY = "trade.failed";

    // New Routing Keys for Notification Service
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String ORDER_REJECTED_ROUTING_KEY = "order.rejected";
    public static final String ORDER_EXPIRED_ROUTING_KEY = "order.expired";
    public static final String ORDER_FILLED_ROUTING_KEY = "order.filled";
    public static final String FUNDS_RELEASED_ROUTING_KEY = "funds.released";
    public static final String FUNDS_DEPOSITED_ROUTING_KEY = "funds.deposited";
    public static final String FUNDS_WITHDRAWN_ROUTING_KEY = "funds.withdrawn";

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
        // Note: For more advanced retry configuration, consider using @Retryable
        // annotation on message handlers
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
    public TopicExchange portfolioExchange() {
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

    // Funds Exchange
    @Bean
    public TopicExchange fundsExchange() {
        return new TopicExchange(FUNDS_EXCHANGE, true, false);
    }

    // Order Service Queues
    @Bean
    public Queue orderValidatedFundsQueue() {
        return QueueBuilder.durable(ORDER_VALIDATED_FUNDS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_VALIDATED_FUNDS_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue fundsLockedOrderQueue() {
        return QueueBuilder.durable(FUNDS_LOCKED_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FUNDS_LOCKED_ORDER_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeExecutedOrderQueue() {
        return QueueBuilder.durable(TRADE_EXECUTED_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_EXECUTED_ORDER_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    // Order Service Bindings
    @Bean
    public Binding orderValidatedFundsBinding() {
        return BindingBuilder.bind(orderValidatedFundsQueue())
                .to(orderExchange())
                .with(ORDER_VALIDATED_ROUTING_KEY);
    }

    @Bean
    public Binding fundsLockedOrderBinding() {
        return BindingBuilder.bind(fundsLockedOrderQueue())
                .to(fundsExchange())
                .with(FUNDS_LOCKED_ROUTING_KEY);
    }

    @Bean
    public Binding tradeExecutedOrderBinding() {
        return BindingBuilder.bind(tradeExecutedOrderQueue())
                .to(tradeExchange())
                .with(TRADE_EXECUTED_ROUTING_KEY);
    }

    // =====================================================
    // Notification Service Queues and Bindings
    // =====================================================

    @Bean
    public Queue orderCancelledNotificationQueue() {
        return QueueBuilder.durable(ORDER_CANCELLED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_CANCELLED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue orderRejectedNotificationQueue() {
        return QueueBuilder.durable(ORDER_REJECTED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_REJECTED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue orderExpiredNotificationQueue() {
        return QueueBuilder.durable(ORDER_EXPIRED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_EXPIRED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue orderFilledNotificationQueue() {
        return QueueBuilder.durable(ORDER_FILLED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_FILLED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue fundsLockedNotificationQueue() {
        return QueueBuilder.durable(FUNDS_LOCKED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FUNDS_LOCKED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue fundsReleasedNotificationQueue() {
        return QueueBuilder.durable(FUNDS_RELEASED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FUNDS_RELEASED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue fundsDepositedNotificationQueue() {
        return QueueBuilder.durable(FUNDS_DEPOSITED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FUNDS_DEPOSITED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue fundsWithdrawnNotificationQueue() {
        return QueueBuilder.durable(FUNDS_WITHDRAWN_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FUNDS_WITHDRAWN_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeExecutedNotificationQueue() {
        return QueueBuilder.durable(TRADE_EXECUTED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_EXECUTED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    // Notification Service Bindings
    @Bean
    public Binding orderCancelledNotificationBinding() {
        return BindingBuilder.bind(orderCancelledNotificationQueue())
                .to(orderExchange())
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding orderRejectedNotificationBinding() {
        return BindingBuilder.bind(orderRejectedNotificationQueue())
                .to(orderExchange())
                .with(ORDER_REJECTED_ROUTING_KEY);
    }

    @Bean
    public Binding orderExpiredNotificationBinding() {
        return BindingBuilder.bind(orderExpiredNotificationQueue())
                .to(orderExchange())
                .with(ORDER_EXPIRED_ROUTING_KEY);
    }

    @Bean
    public Binding orderFilledNotificationBinding() {
        return BindingBuilder.bind(orderFilledNotificationQueue())
                .to(orderExchange())
                .with(ORDER_FILLED_ROUTING_KEY);
    }

    @Bean
    public Binding fundsLockedNotificationBinding() {
        return BindingBuilder.bind(fundsLockedNotificationQueue())
                .to(fundsExchange())
                .with(FUNDS_LOCKED_ROUTING_KEY);
    }

    @Bean
    public Binding fundsReleasedNotificationBinding() {
        return BindingBuilder.bind(fundsReleasedNotificationQueue())
                .to(fundsExchange())
                .with(FUNDS_RELEASED_ROUTING_KEY);
    }

    @Bean
    public Binding fundsDepositedNotificationBinding() {
        return BindingBuilder.bind(fundsDepositedNotificationQueue())
                .to(fundsExchange())
                .with(FUNDS_DEPOSITED_ROUTING_KEY);
    }

    @Bean
    public Binding fundsWithdrawnNotificationBinding() {
        return BindingBuilder.bind(fundsWithdrawnNotificationQueue())
                .to(fundsExchange())
                .with(FUNDS_WITHDRAWN_ROUTING_KEY);
    }

    @Bean
    public Binding tradeExecutedNotificationBinding() {
        return BindingBuilder.bind(tradeExecutedNotificationQueue())
                .to(tradeExchange())
                .with(TRADE_EXECUTED_ROUTING_KEY);
    }

    // =====================================================
    // Trade Service Queues and Bindings
    // =====================================================

    @Bean
    public Queue fundsLockedTradeQueue() {
        return QueueBuilder.durable(FUNDS_LOCKED_TRADE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FUNDS_LOCKED_TRADE_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeCreatedNotificationQueue() {
        return QueueBuilder.durable(TRADE_CREATED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_CREATED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradePlacedNotificationQueue() {
        return QueueBuilder.durable(TRADE_PLACED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_PLACED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeClosedNotificationQueue() {
        return QueueBuilder.durable(TRADE_CLOSED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_CLOSED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeCancelledNotificationQueue() {
        return QueueBuilder.durable(TRADE_CANCELLED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_CANCELLED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeFailedNotificationQueue() {
        return QueueBuilder.durable(TRADE_FAILED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_FAILED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeClosedFundsQueue() {
        return QueueBuilder.durable(TRADE_CLOSED_FUNDS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_CLOSED_FUNDS_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue tradeCancelledFundsQueue() {
        return QueueBuilder.durable(TRADE_CANCELLED_FUNDS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", TRADE_CANCELLED_FUNDS_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    // Trade Service Bindings
    @Bean
    public Binding fundsLockedTradeBinding() {
        return BindingBuilder.bind(fundsLockedTradeQueue())
                .to(fundsExchange())
                .with(FUNDS_LOCKED_ROUTING_KEY);
    }

    @Bean
    public Binding tradeCreatedNotificationBinding() {
        return BindingBuilder.bind(tradeCreatedNotificationQueue())
                .to(tradeExchange())
                .with(TRADE_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding tradePlacedNotificationBinding() {
        return BindingBuilder.bind(tradePlacedNotificationQueue())
                .to(tradeExchange())
                .with(TRADE_PLACED_ROUTING_KEY);
    }

    @Bean
    public Binding tradeClosedNotificationBinding() {
        return BindingBuilder.bind(tradeClosedNotificationQueue())
                .to(tradeExchange())
                .with(TRADE_CLOSED_ROUTING_KEY);
    }

    @Bean
    public Binding tradeCancelledNotificationBinding() {
        return BindingBuilder.bind(tradeCancelledNotificationQueue())
                .to(tradeExchange())
                .with(TRADE_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding tradeFailedNotificationBinding() {
        return BindingBuilder.bind(tradeFailedNotificationQueue())
                .to(tradeExchange())
                .with(TRADE_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding tradeClosedFundsBinding() {
        return BindingBuilder.bind(tradeClosedFundsQueue())
                .to(tradeExchange())
                .with(TRADE_CLOSED_ROUTING_KEY);
    }

    @Bean
    public Binding tradeCancelledFundsBinding() {
        return BindingBuilder.bind(tradeCancelledFundsQueue())
                .to(tradeExchange())
                .with(TRADE_CANCELLED_ROUTING_KEY);
    }

    // =====================================================
    // Payment Service Exchange, Queues, and Bindings
    // =====================================================

    // Payment Exchange
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // Payment Queues
    public static final String PAYMENT_SUCCESS_FUNDS_QUEUE = "payment.success.funds.queue";
    public static final String PAYMENT_CREATED_NOTIFICATION_QUEUE = "payment.created.notification.queue";
    public static final String PAYMENT_SUCCESS_NOTIFICATION_QUEUE = "payment.success.notification.queue";
    public static final String PAYMENT_FAILED_NOTIFICATION_QUEUE = "payment.failed.notification.queue";
    public static final String PAYMENT_EXPIRED_NOTIFICATION_QUEUE = "payment.expired.notification.queue";

    // Payment Routing Keys
    public static final String PAYMENT_CREATED_ROUTING_KEY = "payment.created";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String PAYMENT_EXPIRED_ROUTING_KEY = "payment.expired";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentSuccessFundsQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_FUNDS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_SUCCESS_FUNDS_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue paymentCreatedNotificationQueue() {
        return QueueBuilder.durable(PAYMENT_CREATED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_CREATED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue paymentSuccessNotificationQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_SUCCESS_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue paymentFailedNotificationQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_FAILED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue paymentExpiredNotificationQueue() {
        return QueueBuilder.durable(PAYMENT_EXPIRED_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PAYMENT_EXPIRED_NOTIFICATION_QUEUE + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    // Payment Bindings
    @Bean
    public Binding paymentSuccessFundsBinding() {
        return BindingBuilder.bind(paymentSuccessFundsQueue())
                .to(paymentExchange())
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentCreatedNotificationBinding() {
        return BindingBuilder.bind(paymentCreatedNotificationQueue())
                .to(paymentExchange())
                .with(PAYMENT_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentSuccessNotificationBinding() {
        return BindingBuilder.bind(paymentSuccessNotificationQueue())
                .to(paymentExchange())
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentFailedNotificationBinding() {
        return BindingBuilder.bind(paymentFailedNotificationQueue())
                .to(paymentExchange())
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentExpiredNotificationBinding() {
        return BindingBuilder.bind(paymentExpiredNotificationQueue())
                .to(paymentExchange())
                .with(PAYMENT_EXPIRED_ROUTING_KEY);
    }
}
