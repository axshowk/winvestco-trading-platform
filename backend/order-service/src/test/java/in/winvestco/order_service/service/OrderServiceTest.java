package in.winvestco.order_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderStatus;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.OrderValidity;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.dto.OrderDTO;
import in.winvestco.order_service.exception.OrderNotFoundException;
import in.winvestco.order_service.mapper.OrderMapper;
import in.winvestco.order_service.model.Order;
import in.winvestco.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderValidationService validationService;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private MeterRegistry meterRegistry;

    @Spy
    private ObservationRegistry observationRegistry = ObservationRegistry.create();

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest buyRequest;
    private Order newOrder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "marketCloseHour", 15);
        ReflectionTestUtils.setField(orderService, "marketCloseMinute", 30);
        ReflectionTestUtils.setField(orderService, "timezone", "Asia/Kolkata");

        buyRequest = new CreateOrderRequest();
        buyRequest.setSymbol("RELIANCE");
        buyRequest.setSide(OrderSide.BUY);
        buyRequest.setOrderType(OrderType.LIMIT);
        buyRequest.setQuantity(new BigDecimal("10"));
        buyRequest.setPrice(new BigDecimal("2500"));
        buyRequest.setValidity(OrderValidity.DAY);

        newOrder = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .userId(1L)
                .symbol("RELIANCE")
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("2500"))
                .status(OrderStatus.NEW)
                .filledQuantity(BigDecimal.ZERO)
                .build();

        Counter mockCounter = mock(Counter.class);
        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(mockCounter);
    }

    @Test
    void createOrder_ShouldCreateAndValidateOrder() {
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);
        when(orderMapper.toDTO(any(Order.class))).thenReturn(new OrderDTO());

        OrderDTO result = orderService.createOrder(1L, buyRequest);

        assertNotNull(result);
        verify(validationService).validate(buyRequest);
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
        verify(eventPublisher).publishOrderCreated(any(Order.class));
        verify(eventPublisher).publishOrderValidated(any(Order.class));
    }

    @Test
    void cancelOrder_ShouldUpdateStatus() {
        newOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findByOrderId(anyString())).thenReturn(Optional.of(newOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);
        when(orderMapper.toDTO(any(Order.class))).thenReturn(new OrderDTO());

        orderService.cancelOrder(newOrder.getOrderId(), 1L, "Test reason");

        assertEquals(OrderStatus.CANCELLED, newOrder.getStatus());
        verify(eventPublisher).publishOrderCancelled(eq(newOrder), eq("Test reason"), eq("USER"));
    }

    @Test
    void cancelOrder_WhenNotOwner_ShouldThrowException() {
        when(orderRepository.findByOrderId(anyString())).thenReturn(Optional.of(newOrder));

        assertThrows(OrderNotFoundException.class,
                () -> orderService.cancelOrder(newOrder.getOrderId(), 2L, "Not owner"));
    }

    @Test
    void handleTradeExecuted_ShouldUpdateFilledQuantity() {
        newOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findByOrderId(anyString())).thenReturn(Optional.of(newOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        orderService.handleTradeExecuted(newOrder.getOrderId(), new BigDecimal("5"), new BigDecimal("2505"), true);

        assertEquals(new BigDecimal("5"), newOrder.getFilledQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, newOrder.getStatus());
        verify(eventPublisher).publishOrderFilled(newOrder);
    }
    @Test
    void handleFundsLocked_ShouldUpdateStatus() {
        newOrder.setStatus(OrderStatus.VALIDATED);
        when(orderRepository.findByOrderId(anyString())).thenReturn(Optional.of(newOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        orderService.handleFundsLocked(newOrder.getOrderId(), "lock-123");

        assertEquals(OrderStatus.PENDING, newOrder.getStatus());
        verify(eventPublisher).publishOrderUpdated(newOrder);
    }

    @Test
    void handleOrderRejected_ShouldUpdateStatus() {
        newOrder.setStatus(OrderStatus.NEW);
        when(orderRepository.findByOrderId(anyString())).thenReturn(Optional.of(newOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        orderService.handleOrderRejected(newOrder.getOrderId(), "Insufficient funds");

        assertEquals(OrderStatus.REJECTED, newOrder.getStatus());
        verify(eventPublisher).publishOrderUpdated(newOrder);
    }

    @Test
    void handleTradeExecuted_PartialFill_ShouldUpdateStatus() {
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setFilledQuantity(BigDecimal.ZERO);
        when(orderRepository.findByOrderId(anyString())).thenReturn(Optional.of(newOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        orderService.handleTradeExecuted(newOrder.getOrderId(), new BigDecimal("2"), new BigDecimal("150.00"), true);

        assertEquals(new BigDecimal("2"), newOrder.getFilledQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, newOrder.getStatus());
        verify(eventPublisher).publishOrderFilled(newOrder);
    }

    @Test
    void expireOrders_ShouldExpireEligibleOrders() {
        newOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findExpiredOrders(anyList(), any())).thenReturn(Collections.singletonList(newOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        int expiredCount = orderService.expireOrders();

        assertEquals(1, expiredCount);
        assertEquals(OrderStatus.EXPIRED, newOrder.getStatus());
        verify(eventPublisher).publishOrderExpired(newOrder);
    }

}
