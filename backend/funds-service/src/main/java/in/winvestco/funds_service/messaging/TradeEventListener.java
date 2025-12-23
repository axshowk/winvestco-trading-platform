package in.winvestco.funds_service.messaging;

import com.rabbitmq.client.Channel;
import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.TradeFailedEvent;
import in.winvestco.funds_service.service.FundsLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Listener for trade-related events from RabbitMQ.
 * Handles compensation logic for failed trades (e.g., releasing funds).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TradeEventListener {

    private final FundsLockService fundsLockService;

    /**
     * Handle TradeFailedEvent - release locked funds for the order.
     */
    @RabbitListener(queues = RabbitMQConfig.TRADE_FAILED_FUNDS_QUEUE)
    public void handleTradeFailed(TradeFailedEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received TradeFailedEvent for order: {}, user: {}, reason: {}",
                event.getOrderId(), event.getUserId(), event.getFailureReason());

        try {
            // compensation: release locked funds
            fundsLockService.releaseFunds(
                    event.getOrderId(),
                    "Trade failed: " + event.getFailureReason());

            // Acknowledge message
            channel.basicAck(deliveryTag, false);
            log.info("Successfully released funds for failed trade order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process TradeFailedEvent for order: {}", event.getOrderId(), e);
            try {
                // Reject and requeue for retry
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception nackEx) {
                log.error("Failed to nack message for trade failure order: {}", event.getOrderId(), nackEx);
            }
        }
    }
}
