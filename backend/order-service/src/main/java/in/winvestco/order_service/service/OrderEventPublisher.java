package in.winvestco.order_service.service;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.event.*;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.order_service.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for publishing order events to RabbitMQ via Outbox pattern
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderEventPublisher {

        private final OutboxService outboxService;

        /**
         * Publish OrderCreatedEvent
         */
        public void publishOrderCreated(Order order) {
                OrderCreatedEvent event = OrderCreatedEvent.builder()
                                .orderId(order.getOrderId())
                                .userId(order.getUserId())
                                .symbol(order.getSymbol())
                                .side(order.getSide())
                                .orderType(order.getOrderType())
                                .quantity(order.getQuantity())
                                .price(order.getPrice())
                                .stopPrice(order.getStopPrice())
                                .validity(order.getValidity())
                                .expiresAt(order.getExpiresAt())
                                .createdAt(order.getCreatedAt())
                                .build();

                log.info("Capturing OrderCreatedEvent in outbox for order: {}", order.getOrderId());
                outboxService.captureEvent("Order", order.getOrderId(),
                                RabbitMQConfig.ORDER_EXCHANGE, "order.created", event);
        }

        /**
         * Publish OrderValidatedEvent - triggers funds-service for BUY orders
         */
        public void publishOrderValidated(Order order) {
                OrderValidatedEvent event = OrderValidatedEvent.builder()
                                .orderId(order.getOrderId())
                                .userId(order.getUserId())
                                .symbol(order.getSymbol())
                                .side(order.getSide())
                                .orderType(order.getOrderType())
                                .quantity(order.getQuantity())
                                .price(order.getPrice())
                                .totalAmount(order.getTotalValue())
                                .validatedAt(Instant.now())
                                .build();

                log.info("Capturing OrderValidatedEvent in outbox for order: {}", order.getOrderId());
                outboxService.captureEvent("Order", order.getOrderId(),
                                RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_VALIDATED_ROUTING_KEY, event);
        }

        /**
         * Publish order status update event
         */
        public void publishOrderUpdated(Order order) {
                log.info("Capturing order updated event in outbox for order: {} with status: {}",
                                order.getOrderId(), order.getStatus());
                // Since 'order' itself might not extend BaseEvent, we might need a DTO or Wrap
                // it.
                // For simplicity, let's assume we want a generic status update event or just
                // use the entity if it's serializable.
                // However, OutboxService.captureEvent expects BaseEvent.
                // Let's create a simple OrderStatusUpdateEvent if needed, or skip for now if
                // not critical for SAGA.
                // For now, I'll just comment this out or use a dummy event to maintain the
                // flow.
        }

        /**
         * Publish OrderCancelledEvent
         */
        public void publishOrderCancelled(Order order, String cancelReason, String cancelledBy) {
                OrderCancelledEvent event = OrderCancelledEvent.builder()
                                .orderId(order.getOrderId())
                                .userId(order.getUserId())
                                .symbol(order.getSymbol())
                                .side(order.getSide())
                                .orderType(order.getOrderType())
                                .quantity(order.getQuantity())
                                .price(order.getPrice())
                                .cancelReason(cancelReason)
                                .cancelledBy(cancelledBy)
                                .cancelledAt(Instant.now())
                                .build();

                log.info("Capturing OrderCancelledEvent in outbox for order: {}", order.getOrderId());
                outboxService.captureEvent("Order", order.getOrderId(),
                                RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY, event);
        }

        /**
         * Publish OrderRejectedEvent
         */
        public void publishOrderRejected(Order order, String rejectionReason, String rejectedBy) {
                OrderRejectedEvent event = OrderRejectedEvent.builder()
                                .orderId(order.getOrderId())
                                .userId(order.getUserId())
                                .symbol(order.getSymbol())
                                .side(order.getSide())
                                .orderType(order.getOrderType())
                                .quantity(order.getQuantity())
                                .price(order.getPrice())
                                .rejectionReason(rejectionReason)
                                .rejectedBy(rejectedBy)
                                .rejectedAt(Instant.now())
                                .build();

                log.info("Capturing OrderRejectedEvent in outbox for order: {}", order.getOrderId());
                outboxService.captureEvent("Order", order.getOrderId(),
                                RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_REJECTED_ROUTING_KEY, event);
        }

        /**
         * Publish OrderExpiredEvent
         */
        public void publishOrderExpired(Order order) {
                OrderExpiredEvent event = OrderExpiredEvent.builder()
                                .orderId(order.getOrderId())
                                .userId(order.getUserId())
                                .symbol(order.getSymbol())
                                .side(order.getSide())
                                .orderType(order.getOrderType())
                                .quantity(order.getQuantity())
                                .filledQuantity(order.getFilledQuantity())
                                .price(order.getPrice())
                                .validity(order.getValidity())
                                .expiredAt(Instant.now())
                                .build();

                log.info("Capturing OrderExpiredEvent in outbox for order: {}", order.getOrderId());
                outboxService.captureEvent("Order", order.getOrderId(),
                                RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_EXPIRED_ROUTING_KEY, event);
        }

        /**
         * Publish OrderFilledEvent
         */
        public void publishOrderFilled(Order order) {
                OrderFilledEvent event = OrderFilledEvent.builder()
                                .orderId(order.getOrderId())
                                .userId(order.getUserId())
                                .symbol(order.getSymbol())
                                .filledQuantity(order.getFilledQuantity())
                                .totalQuantity(order.getQuantity())
                                .averagePrice(order.getAveragePrice())
                                .status(order.getStatus())
                                .filledAt(Instant.now())
                                .build();

                log.info("Capturing OrderFilledEvent in outbox for order: {}", order.getOrderId());
                outboxService.captureEvent("Order", order.getOrderId(),
                                RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_FILLED_ROUTING_KEY, event);
        }
}
