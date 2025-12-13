package in.winvestco.funds_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for funds service
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.user:user.events}")
    private String userExchange;

    @Value("${rabbitmq.queues.user-created:user.created.funds}")
    private String userCreatedQueue;

    @Value("${rabbitmq.routing-keys.user-created:user.created}")
    private String userCreatedRoutingKey;

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(userExchange);
    }

    @Bean
    public Queue userCreatedFundsQueue() {
        return new Queue(userCreatedQueue, true);
    }

    @Bean
    public Binding userCreatedBinding(Queue userCreatedFundsQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userCreatedFundsQueue)
                .to(userExchange)
                .with(userCreatedRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
