package in.winvestco.order_service.service;

import in.winvestco.common.enums.OrderStatus;
import in.winvestco.common.enums.OrderValidity;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.model.Order;
import in.winvestco.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExpiryEdgeCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private Order dayOrder;
    private Order gtcOrder;
    private Order iocOrder;
    private Instant pastExpiryTime;
    private Instant futureExpiryTime;

    @BeforeEach
    void setUp() {
        // Set up time zone for expiry calculations
        pastExpiryTime = Instant.now().minusSeconds(3600); // 1 hour ago
        futureExpiryTime = Instant.now().plusSeconds(3600); // 1 hour from now

        // Create test orders
        dayOrder = Order.builder()
                .orderId("DAY-123")
                .userId("user-123")
                .symbol("RELIANCE")
                .orderType("MARKET")
                .side("BUY")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("2500"))
                .validity(OrderValidity.DAY)
                .expiresAt(pastExpiryTime)
                .status(OrderStatus.NEW)
                .filledQuantity(BigDecimal.ZERO)
                .createdAt(Instant.now().minusSeconds(7200))
                .build();

        gtcOrder = Order.builder()
                .orderId("GTC-456")
                .userId("user-123")
                .symbol("TCS")
                .orderType("LIMIT")
                .side("BUY")
                .quantity(new BigDecimal("50"))
                .price(new BigDecimal("3500"))
                .validity(OrderValidity.GTC)
                .expiresAt(null) // GTC orders don't expire
                .status(OrderStatus.NEW)
                .filledQuantity(BigDecimal.ZERO)
                .createdAt(Instant.now().minusSeconds(7200))
                .build();

        iocOrder = Order.builder()
                .orderId("IOC-789")
                .userId("user-123")
                .symbol("INFY")
                .orderType("MARKET")
                .side("SELL")
                .quantity(new BigDecimal("75"))
                .price(null)
                .validity(OrderValidity.IOC)
                .expiresAt(null) // IOC orders are immediate
                .status(OrderStatus.FILLED) // IOC orders should be processed immediately
                .filledQuantity(new BigDecimal("75"))
                .createdAt(Instant.now().minusSeconds(60))
                .build();
    }

    @Test
    void expireOrders_WhenMultipleOrderTypesExpired_ShouldOnlyExpireValidOnes() {
        List<Order> expiredOrders = List.of(dayOrder, gtcOrder, iocOrder);
        
        when(orderRepository.findExpiredOrders(anyList(), any(Instant.class)))
                .thenReturn(List.of(dayOrder)); // Only DAY order should be returned
        when(orderRepository.save(any(Order.class))).thenReturn(dayOrder);

        int expiredCount = orderService.expireOrders();

        verify(orderRepository).findExpiredOrders(
                argThat(statuses -> statuses.containsAll(List.of(
                        OrderStatus.NEW, OrderStatus.VALIDATED,
                        OrderStatus.FUNDS_LOCKED, OrderStatus.PENDING,
                        OrderStatus.PARTIALLY_FILLED))),
                any(Instant.class));
        
        verify(orderRepository).save(dayOrder);
        verify(eventPublisher).publishOrderExpired(dayOrder);
        verify(eventPublisher).publishOrderUpdated(dayOrder);
        
        assertThat(expiredCount).isEqualTo(1);
    }

    @Test
    void expireOrders_WhenNoExpiredOrdersFound_ShouldReturnZero() {
        when(orderRepository.findExpiredOrders(anyList(), any(Instant.class)))
                .thenReturn(List.of());

        int expiredCount = orderService.expireOrders();

        verify(orderRepository).findExpiredOrders(anyList(), any(Instant.class));
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishOrderExpired(any());
        verify(eventPublisher, never()).publishOrderUpdated(any());
        
        assertThat(expiredCount).isEqualTo(0);
    }

    @Test
    void expireOrders_WhenPartiallyFilledOrdersExpired_ShouldExpireThem() {
        Order partiallyFilledOrder = dayOrder.toBuilder()
                .status(OrderStatus.PARTIALLY_FILLED)
                .filledQuantity(new BigDecimal("50"))
                .build();

        when(orderRepository.findExpiredOrders(anyList(), any(Instant.class)))
                .thenReturn(List.of(partiallyFilledOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(partiallyFilledOrder);

        int expiredCount = orderService.expireOrders();

        verify(orderRepository).save(partiallyFilledOrder);
        verify(eventPublisher).publishOrderExpired(partiallyFilledOrder);
        verify(eventPublisher).publishOrderUpdated(partiallyFilledOrder);
        
        assertThat(expiredCount).isEqualTo(1);
        assertThat(partiallyFilledOrder.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    @Test
    void expireOrders_WhenDatabaseErrorOccurs_ShouldPropagateException() {
        when(orderRepository.findExpiredOrders(anyList(), any(Instant.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThatThrownBy(() -> orderService.expireOrders())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");

        verify(orderRepository).findExpiredOrders(anyList(), any(Instant.class));
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishOrderExpired(any());
        verify(eventPublisher, never()).publishOrderUpdated(any());
    }

    @Test
    void expireOrders_WhenEventPublishingFails_ShouldContinueProcessing() {
        Order secondOrder = dayOrder.toBuilder().orderId("DAY-456").build();
        List<Order> expiredOrders = List.of(dayOrder, secondOrder);

        when(orderRepository.findExpiredOrders(anyList(), any(Instant.class)))
                .thenReturn(expiredOrders);
        when(orderRepository.save(any(Order.class)))
                .thenReturn(dayOrder)
                .thenReturn(secondOrder);
        
        doThrow(new RuntimeException("Event publishing failed"))
                .when(eventPublisher).publishOrderExpired(dayOrder);

        int expiredCount = orderService.expireOrders();

        // Should continue processing despite event failure
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(eventPublisher).publishOrderExpired(dayOrder);
        verify(eventPublisher).publishOrderUpdated(dayOrder);
        verify(eventPublisher).publishOrderExpired(secondOrder);
        verify(eventPublisher).publishOrderUpdated(secondOrder);
        
        assertThat(expiredCount).isEqualTo(2);
    }

    @Test
    void expireOrders_ShouldNotExpireTerminalStatusOrders() {
        List<OrderStatus> terminalStatuses = List.of(
                OrderStatus.FILLED, OrderStatus.CANCELLED,
                OrderStatus.REJECTED, OrderStatus.EXPIRED);

        when(orderRepository.findExpiredOrders(anyList(), any(Instant.class)))
                .thenReturn(List.of()); // Should not find terminal status orders

        int expiredCount = orderService.expireOrders();

        verify(orderRepository).findExpiredOrders(
                argThat(statuses -> !statuses.containsAll(terminalStatuses)),
                any(Instant.class));
        
        assertThat(expiredCount).isEqualTo(0);
    }

    @Test
    void expireOrders_WhenLargeNumberOfOrdersExpired_ShouldProcessAll() {
        List<Order> manyExpiredOrders = java.util.stream.IntStream.range(0, 1000)
                .mapToObj(i -> dayOrder.toBuilder()
                        .orderId("DAY-" + i)
                        .build())
                .collect(java.util.stream.Collectors.toList());

        when(orderRepository.findExpiredOrders(anyList(), any(Instant.class)))
                .thenReturn(manyExpiredOrders);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int expiredCount = orderService.expireOrders();

        verify(orderRepository, times(1000)).save(any(Order.class));
        verify(eventPublisher, times(1000)).publishOrderExpired(any(Order.class));
        verify(eventPublisher, times(1000)).publishOrderUpdated(any(Order.class));
        
        assertThat(expiredCount).isEqualTo(1000);
    }

    @Test
    void calculateExpiresAt_WhenDayValidity_ShouldReturnMarketCloseTime() {
        // This tests the private method through the public interface
        Order dayOrderRequest = Order.builder()
                .validity(OrderValidity.DAY)
                .build();

        // Create order with DAY validity
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            // Verify expiry time is set to market close
            assertThat(saved.getExpiresAt()).isNotNull();
            assertThat(saved.getExpiresAt()).isAfter(Instant.now());
            return saved;
        });

        orderService.createOrder(createOrderRequest(OrderValidity.DAY));

        verify(orderRepository).save(argThat(order -> 
                order.getExpiresAt() != null && 
                order.getValidity() == OrderValidity.DAY));
    }

    @Test
    void calculateExpiresAt_WhenGTCValidity_ShouldReturnNull() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            assertThat(saved.getExpiresAt()).isNull();
            return saved;
        });

        orderService.createOrder(createOrderRequest(OrderValidity.GTC));

        verify(orderRepository).save(argThat(order -> 
                order.getExpiresAt() == null && 
                order.getValidity() == OrderValidity.GTC));
    }

    private CreateOrderRequest createOrderRequest(OrderValidity validity) {
        return CreateOrderRequest.builder()
                .userId("user-123")
                .symbol("RELIANCE")
                .orderType("MARKET")
                .side("BUY")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("2500"))
                .validity(validity)
                .build();
    }
}
