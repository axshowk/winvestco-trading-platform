package in.winvestco.order_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.TradeExecutedEvent;
import in.winvestco.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for trade-related events from RabbitMQ
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TradeEventListener {

    private final OrderService orderService;

    /**
     * Handle TradeExecutedEvent - update order fill status
     */
    @RabbitListener(queues = RabbitMQConfig.TRADE_EXECUTED_ORDER_QUEUE)
    public void handleTradeExecuted(TradeExecutedEvent event) {
        log.info("Received TradeExecutedEvent for order: {}, quantity: {}", 
                event.getOrderId(), event.getExecutedQuantity());

        try {
            orderService.handleTradeExecuted(
                    event.getOrderId(),
                    event.getExecutedQuantity(),
                    event.getExecutedPrice(),
                    event.isPartialFill()
            );
            log.info("Successfully processed TradeExecutedEvent for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process TradeExecutedEvent for order: {}", event.getOrderId(), e);
            throw e; // Rethrow to trigger retry/DLQ
        }
    }
}
