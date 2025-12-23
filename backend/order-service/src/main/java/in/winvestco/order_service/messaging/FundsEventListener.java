package in.winvestco.order_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.FundsLockedEvent;
import in.winvestco.common.event.OrderRejectedEvent;
import in.winvestco.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for funds-related events from RabbitMQ
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FundsEventListener {

    private final OrderService orderService;

    /**
     * Handle FundsLockedEvent - transition order to FUNDS_LOCKED
     */
    @RabbitListener(queues = RabbitMQConfig.FUNDS_LOCKED_ORDER_QUEUE)
    public void handleFundsLocked(FundsLockedEvent event) {
        log.info("Received FundsLockedEvent for order: {}", event.getOrderId());

        try {
            orderService.handleFundsLocked(event.getOrderId(), event.getLockId());
            log.info("Successfully processed FundsLockedEvent for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process FundsLockedEvent for order: {}", event.getOrderId(), e);
            throw e; // Rethrow to trigger retry/DLQ
        }
    }

    /**
     * Handle OrderRejectedEvent - transition order to REJECTED
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_REJECTED_ORDER_QUEUE)
    public void handleOrderRejected(OrderRejectedEvent event) {
        log.info("Received OrderRejectedEvent for order: {} from {}",
                event.getOrderId(), event.getRejectedBy());

        try {
            orderService.handleOrderRejected(event.getOrderId(), event.getRejectionReason());
            log.info("Successfully processed OrderRejectedEvent for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process OrderRejectedEvent for order: {}", event.getOrderId(), e);
            throw e; // Rethrow to trigger retry/DLQ
        }
    }
}
