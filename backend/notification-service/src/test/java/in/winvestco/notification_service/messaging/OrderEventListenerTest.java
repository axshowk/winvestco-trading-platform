package in.winvestco.notification_service.messaging;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderStatus;
import in.winvestco.common.enums.OrderValidity;
import in.winvestco.common.event.*;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Test
    void handleOrderCreated_shouldCreateNotification() {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId("ORD-001")
                .userId(1L)
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .quantity(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(2500))
                .build();

        orderEventListener.handleOrderCreated(event);

        verify(notificationService).createNotification(
                eq(1L),
                eq(NotificationType.ORDER_CREATED),
                eq("Order Placed"),
                contains("RELIANCE"),
                anyMap());
    }

    @Test
    void handleOrderFilled_fullyFilled_shouldCreateNotification() {
        OrderFilledEvent event = OrderFilledEvent.builder()
                .orderId("ORD-002")
                .userId(1L)
                .symbol("TCS")
                .filledQuantity(BigDecimal.valueOf(5))
                .totalQuantity(BigDecimal.valueOf(5))
                .averagePrice(BigDecimal.valueOf(3500))
                .status(OrderStatus.FILLED)
                .build();

        orderEventListener.handleOrderFilled(event);

        verify(notificationService).createNotification(
                eq(1L),
                eq(NotificationType.ORDER_FILLED),
                eq("Order Executed"),
                contains("TCS"),
                anyMap());
    }

    @Test
    void handleOrderFilled_partiallyFilled_shouldCreatePartialNotification() {
        OrderFilledEvent event = OrderFilledEvent.builder()
                .orderId("ORD-003")
                .userId(1L)
                .symbol("INFY")
                .filledQuantity(BigDecimal.valueOf(3))
                .totalQuantity(BigDecimal.valueOf(10))
                .averagePrice(BigDecimal.valueOf(1500))
                .status(OrderStatus.PARTIALLY_FILLED)
                .build();

        orderEventListener.handleOrderFilled(event);

        verify(notificationService).createNotification(
                eq(1L),
                eq(NotificationType.ORDER_PARTIALLY_FILLED),
                eq("Order Partially Filled"),
                contains("INFY"),
                anyMap());
    }

    @Test
    void handleOrderCancelled_shouldCreateNotification() {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId("ORD-004")
                .userId(1L)
                .symbol("HDFC")
                .cancelReason("User requested cancellation")
                .cancelledBy("USER")
                .build();

        orderEventListener.handleOrderCancelled(event);

        verify(notificationService).createNotification(
                eq(1L),
                eq(NotificationType.ORDER_CANCELLED),
                eq("Order Cancelled"),
                contains("HDFC"),
                anyMap());
    }

    @Test
    void handleOrderRejected_shouldCreateNotification() {
        OrderRejectedEvent event = OrderRejectedEvent.builder()
                .orderId("ORD-005")
                .userId(1L)
                .symbol("SBIN")
                .rejectionReason("Insufficient funds")
                .rejectedBy("SYSTEM")
                .build();

        orderEventListener.handleOrderRejected(event);

        verify(notificationService).createNotification(
                eq(1L),
                eq(NotificationType.ORDER_REJECTED),
                eq("Order Rejected"),
                contains("SBIN"),
                anyMap());
    }

    @Test
    void handleOrderExpired_shouldCreateNotification() {
        OrderExpiredEvent event = OrderExpiredEvent.builder()
                .orderId("ORD-006")
                .userId(1L)
                .symbol("WIPRO")
                .validity(OrderValidity.DAY)
                .build();

        orderEventListener.handleOrderExpired(event);

        verify(notificationService).createNotification(
                eq(1L),
                eq(NotificationType.ORDER_EXPIRED),
                eq("Order Expired"),
                contains("WIPRO"),
                anyMap());
    }
}
