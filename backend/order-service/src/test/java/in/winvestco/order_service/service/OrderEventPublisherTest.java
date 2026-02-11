package in.winvestco.order_service.service;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderStatus;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.OrderValidity;
import in.winvestco.common.event.*;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.order_service.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OrderEventPublisher eventPublisher;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .orderId("12345")
                .userId(1L)
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(new BigDecimal("10"))
                .filledQuantity(new BigDecimal("0"))
                .price(new BigDecimal("150.00"))
                .stopPrice(new BigDecimal("0"))
                .validity(OrderValidity.DAY)
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void publishOrderCreated_ShouldCaptureEvent() {
        eventPublisher.publishOrderCreated(order);

        verify(outboxService).captureEvent(
                eq("Order"),
                eq("12345"),
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq("order.created"),
                any(OrderCreatedEvent.class)
        );
    }

    @Test
    void publishOrderValidated_ShouldCaptureEvent() {
        eventPublisher.publishOrderValidated(order);

        verify(outboxService).captureEvent(
                eq("Order"),
                eq("12345"),
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_VALIDATED_ROUTING_KEY),
                any(OrderValidatedEvent.class)
        );
    }

    @Test
    void publishOrderUpdated_ShouldLog() {
        // Method is currently only logging, just ensure no exception
        eventPublisher.publishOrderUpdated(order);
    }

    @Test
    void publishOrderCancelled_ShouldCaptureEvent() {
        eventPublisher.publishOrderCancelled(order, "User Request", "User");

        verify(outboxService).captureEvent(
                eq("Order"),
                eq("12345"),
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY),
                any(OrderCancelledEvent.class)
        );
    }

    @Test
    void publishOrderRejected_ShouldCaptureEvent() {
        eventPublisher.publishOrderRejected(order, "Insufficient Funds", "System");

        verify(outboxService).captureEvent(
                eq("Order"),
                eq("12345"),
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_REJECTED_ROUTING_KEY),
                any(OrderRejectedEvent.class)
        );
    }

    @Test
    void publishOrderExpired_ShouldCaptureEvent() {
        eventPublisher.publishOrderExpired(order);

        verify(outboxService).captureEvent(
                eq("Order"),
                eq("12345"),
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_EXPIRED_ROUTING_KEY),
                any(OrderExpiredEvent.class)
        );
    }

    @Test
    void publishOrderFilled_ShouldCaptureEvent() {
        eventPublisher.publishOrderFilled(order);

        verify(outboxService).captureEvent(
                eq("Order"),
                eq("12345"),
                eq(RabbitMQConfig.ORDER_EXCHANGE),
                eq(RabbitMQConfig.ORDER_FILLED_ROUTING_KEY),
                any(OrderFilledEvent.class)
        );
    }
}
