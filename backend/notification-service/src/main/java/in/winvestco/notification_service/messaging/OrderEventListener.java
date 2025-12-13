package in.winvestco.notification_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.*;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for order-related events from RabbitMQ.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_NOTIFICATION_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getOrderId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("symbol", event.getSymbol());
        data.put("side", event.getSide().name());
        data.put("quantity", event.getQuantity());
        data.put("price", event.getPrice());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.ORDER_CREATED,
            "Order Placed",
            String.format("Your %s order for %s %s at ₹%s has been placed.",
                event.getSide(), event.getQuantity(), event.getSymbol(), event.getPrice()),
            data
        );
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_FILLED_NOTIFICATION_QUEUE)
    public void handleOrderFilled(OrderFilledEvent event) {
        log.info("Received OrderFilledEvent for order: {}", event.getOrderId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("symbol", event.getSymbol());
        data.put("filledQuantity", event.getFilledQuantity());
        data.put("totalQuantity", event.getTotalQuantity());
        data.put("averagePrice", event.getAveragePrice());

        NotificationType type = event.getStatus().name().equals("PARTIALLY_FILLED") 
            ? NotificationType.ORDER_PARTIALLY_FILLED 
            : NotificationType.ORDER_FILLED;
        
        String title = type == NotificationType.ORDER_PARTIALLY_FILLED 
            ? "Order Partially Filled" 
            : "Order Executed";
            
        String message = type == NotificationType.ORDER_PARTIALLY_FILLED
            ? String.format("Your order for %s has been partially filled: %s of %s shares at avg ₹%s",
                event.getSymbol(), event.getFilledQuantity(), event.getTotalQuantity(), event.getAveragePrice())
            : String.format("Your order for %s shares of %s has been executed at avg ₹%s",
                event.getFilledQuantity(), event.getSymbol(), event.getAveragePrice());

        notificationService.createNotification(event.getUserId(), type, title, message, data);
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_NOTIFICATION_QUEUE)
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order: {}", event.getOrderId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("symbol", event.getSymbol());
        data.put("cancelReason", event.getCancelReason());
        data.put("cancelledBy", event.getCancelledBy());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.ORDER_CANCELLED,
            "Order Cancelled",
            String.format("Your order for %s has been cancelled. Reason: %s",
                event.getSymbol(), event.getCancelReason()),
            data
        );
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_REJECTED_NOTIFICATION_QUEUE)
    public void handleOrderRejected(OrderRejectedEvent event) {
        log.info("Received OrderRejectedEvent for order: {}", event.getOrderId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("symbol", event.getSymbol());
        data.put("rejectionReason", event.getRejectionReason());
        data.put("rejectedBy", event.getRejectedBy());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.ORDER_REJECTED,
            "Order Rejected",
            String.format("Your order for %s has been rejected. Reason: %s",
                event.getSymbol(), event.getRejectionReason()),
            data
        );
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_EXPIRED_NOTIFICATION_QUEUE)
    public void handleOrderExpired(OrderExpiredEvent event) {
        log.info("Received OrderExpiredEvent for order: {}", event.getOrderId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("symbol", event.getSymbol());
        data.put("validity", event.getValidity().name());

        notificationService.createNotification(
            event.getUserId(),
            NotificationType.ORDER_EXPIRED,
            "Order Expired",
            String.format("Your %s order for %s has expired.",
                event.getValidity(), event.getSymbol()),
            data
        );
    }
}
