package in.winvestco.funds_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static in.winvestco.common.config.RabbitMQConfig.DLQ_EXCHANGE;

/**
 * Funds-service specific RabbitMQ configuration.
 * Uses exchanges defined in common module and only defines service-specific
 * queues.
 */
@Configuration("fundsServiceRabbitMQConfig")
public class RabbitMQConfig {

    @Value("${rabbitmq.queues.user-created:user.created.funds}")
    private String userCreatedQueue;

    @Value("${rabbitmq.routing-keys.user-created:user.created}")
    private String userCreatedRoutingKey;

    /**
     * Queue for receiving user created events in funds service.
     * Uses unique queue name to avoid conflict with common module's user queues.
     */
    @Bean("userCreatedFundsQueue")
    public Queue userCreatedFundsQueue() {
        return QueueBuilder.durable(userCreatedQueue)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", userCreatedQueue + ".dlq")
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    /**
     * Binding for user created events to funds service queue.
     * Uses the userExchange from common module.
     */
    @Bean("userCreatedFundsBinding")
    public Binding userCreatedFundsBinding(
            @Qualifier("userCreatedFundsQueue") Queue userCreatedFundsQueue,
            @Qualifier("userExchange") TopicExchange userExchange) {
        return BindingBuilder.bind(userCreatedFundsQueue)
                .to(userExchange)
                .with(userCreatedRoutingKey);
    }
}
