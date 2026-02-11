package in.winvestco.order_service.service;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.OrderValidity;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.mapper.OrderMapper;
import in.winvestco.order_service.model.Order;
import in.winvestco.order_service.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceObservabilityTest {

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

    @Mock
    private Counter counter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "marketCloseHour", 15);
        ReflectionTestUtils.setField(orderService, "marketCloseMinute", 30);
        ReflectionTestUtils.setField(orderService, "timezone", "Asia/Kolkata");

        // Setup ObservationRegistry to return a NOOP observation or just mock the
        // create
        lenient().when(observationRegistry.getCurrentObservationScope()).thenReturn(null);
    }

    @Test
    void createOrder_ShouldRecordMetricsAndObservation() {
        // Arrange
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSymbol("AAPL");
        request.setSide(OrderSide.BUY);
        request.setOrderType(OrderType.LIMIT);
        request.setQuantity(new BigDecimal("10"));
        request.setPrice(new BigDecimal("150"));
        request.setValidity(OrderValidity.DAY);

        Order order = Order.builder()
                .orderId("test-order-id")
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        // Act
        orderService.createOrder(userId, request);

        // Assert
        verify(meterRegistry).counter(eq("orders.count"), any(String[].class));
        verify(counter).increment();
        verify(observationRegistry).observationConfig(); // Basic check that observation registry was touched
    }
}
