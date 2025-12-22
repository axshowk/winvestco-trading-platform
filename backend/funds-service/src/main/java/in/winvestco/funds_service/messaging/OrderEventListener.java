package in.winvestco.funds_service.messaging;

import com.rabbitmq.client.Channel;
import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.OrderValidatedEvent;
import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.exception.InsufficientFundsException;
import in.winvestco.funds_service.repository.WalletRepository;
import in.winvestco.funds_service.service.FundsEventPublisher;
import in.winvestco.funds_service.service.FundsLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Listener for order-related events from RabbitMQ.
 * Handles funds locking for BUY orders when orders are validated.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final FundsLockService fundsLockService;
    private final FundsEventPublisher fundsEventPublisher;
    private final WalletRepository walletRepository;

    /**
     * Handle OrderValidatedEvent - lock funds for BUY orders.
     * If insufficient funds, publishes OrderRejectedEvent.
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_VALIDATED_FUNDS_QUEUE)
    public void handleOrderValidated(OrderValidatedEvent event, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received OrderValidatedEvent for order: {}, user: {}, amount: {}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount());

        try {
            // Lock funds for the order
            FundsLockDTO lock = fundsLockService.lockFunds(
                    event.getUserId(),
                    event.getOrderId(),
                    event.getTotalAmount(),
                    "Order placed: " + event.getSymbol() + " " + event.getSide());

            // Get wallet for publishing event with details
            walletRepository.findByUserId(event.getUserId()).ifPresent(wallet -> {
                fundsEventPublisher.publishFundsLockedWithDetails(
                        event.getUserId(),
                        wallet,
                        lock,
                        event.getSymbol(),
                        event.getSide(),
                        event.getOrderType(),
                        event.getQuantity(),
                        event.getPrice());
            });

            // Acknowledge message
            channel.basicAck(deliveryTag, false);
            log.info("Successfully locked funds for order: {}", event.getOrderId());

        } catch (InsufficientFundsException e) {
            log.warn("Insufficient funds for order: {}. Requested: {}, Available: {}",
                    event.getOrderId(), e.getRequested(), e.getAvailable());

            // Publish order rejected event
            fundsEventPublisher.publishOrderRejected(
                    event.getOrderId(),
                    event.getUserId(),
                    event.getSymbol(),
                    event.getSide(),
                    event.getOrderType(),
                    event.getQuantity(),
                    event.getPrice(),
                    String.format("Insufficient funds: requested %.2f, available %.2f",
                            e.getRequested(), e.getAvailable()));

            // Acknowledge message (don't retry - this is a business rule rejection)
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception ackEx) {
                log.error("Failed to ack message for rejected order: {}", event.getOrderId(), ackEx);
            }

        } catch (Exception e) {
            log.error("Failed to process OrderValidatedEvent for order: {}", event.getOrderId(), e);
            try {
                // Reject and requeue for retry
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception nackEx) {
                log.error("Failed to nack message for order: {}", event.getOrderId(), nackEx);
            }
        }
    }
}
