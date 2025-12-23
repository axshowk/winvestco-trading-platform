package in.winvestco.funds_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.OrderCancelledEvent;
import in.winvestco.common.event.OrderValidatedEvent;
import in.winvestco.common.messaging.idempotency.IdempotencyService;
import in.winvestco.funds_service.dto.FundsLockDTO;
import in.winvestco.funds_service.exception.InsufficientFundsException;
import in.winvestco.funds_service.repository.WalletRepository;
import in.winvestco.funds_service.service.FundsEventPublisher;
import in.winvestco.funds_service.service.FundsLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final IdempotencyService idempotencyService;

    /**
     * Handle OrderValidatedEvent - lock funds for BUY orders.
     * If insufficient funds, publishes OrderRejectedEvent.
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_VALIDATED_FUNDS_QUEUE)
    @Transactional
    public void handleOrderValidated(OrderValidatedEvent event) {
        log.info("Received OrderValidatedEvent for order: {}, correlationId: {}",
                event.getOrderId(), event.getCorrelationId());

        if (idempotencyService.exists(event.getCorrelationId())) {
            log.warn("Skipping already processed event: {}", event.getCorrelationId());
            return;
        }

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

            // Mark as processed
            idempotencyService.markAsProcessed(event.getCorrelationId(), "FundsService-OrderValidated");
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

            // Still mark as processed even if rejected (business logic success)
            idempotencyService.markAsProcessed(event.getCorrelationId(), "FundsService-OrderValidated-Rejected");
        } catch (Exception e) {
            log.error("Error processing OrderValidatedEvent for order: {}. Will be retried.", event.getOrderId(), e);
            throw e; // Rethrow to trigger RabbitMQ retry
        }
    }

    /**
     * Handle OrderCancelledEvent - release locked funds for the order.
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_FUNDS_QUEUE)
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order: {}, correlationId: {}",
                event.getOrderId(), event.getCorrelationId());

        if (idempotencyService.exists(event.getCorrelationId())) {
            log.warn("Skipping already processed event: {}", event.getCorrelationId());
            return;
        }

        try {
            // compensation: release locked funds
            fundsLockService.releaseFunds(
                    event.getOrderId(),
                    "Order cancelled: " + event.getCancelReason());

            // Mark as processed
            idempotencyService.markAsProcessed(event.getCorrelationId(), "FundsService-OrderCancelled");
            log.info("Successfully released funds for cancelled order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Error processing OrderCancelledEvent for order: {}. Will be retried.", event.getOrderId(), e);
            throw e; // Rethrow to trigger RabbitMQ retry
        }
    }
}
