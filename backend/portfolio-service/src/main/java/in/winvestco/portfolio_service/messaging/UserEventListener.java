package in.winvestco.portfolio_service.messaging;

import com.rabbitmq.client.Channel;
import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.UserCreatedEvent;
import in.winvestco.portfolio_service.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RabbitMQ listener for user events.
 * Creates a demo portfolio when a new user is registered.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventListener {

    private final PortfolioService portfolioService;

    /**
     * Handle UserCreatedEvent to create a demo portfolio for new users.
     */
    @RabbitListener(queues = RabbitMQConfig.USER_CREATED_PORTFOLIO_QUEUE)
    public void handleUserCreatedEvent(
            UserCreatedEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("Received UserCreatedEvent: userId={}, email={}", event.getUserId(), event.getEmail());

        try {
            // Create demo portfolio for the new user
            portfolioService.createPortfolioForUser(event.getUserId(), event.getEmail());

            // Acknowledge the message
            channel.basicAck(deliveryTag, false);
            log.info("Successfully processed UserCreatedEvent for user: {}", event.getUserId());

        } catch (Exception e) {
            log.error("Error processing UserCreatedEvent for user {}: {}", event.getUserId(), e.getMessage(), e);
            try {
                // Reject and requeue the message
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("Failed to nack message: {}", ioException.getMessage());
            }
        }
    }
}
