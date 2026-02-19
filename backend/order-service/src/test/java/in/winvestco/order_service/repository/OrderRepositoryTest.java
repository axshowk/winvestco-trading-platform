package in.winvestco.order_service.repository;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderStatus;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.OrderValidity;
import in.winvestco.order_service.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Order order1;
    private Order order2;
    private Order order3;
    private static final Long USER_ID_1 = 1L;
    private static final Long USER_ID_2 = 2L;

    @BeforeEach
    void setUp() {
        order1 = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .userId(USER_ID_1)
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("150.00"))
                .status(OrderStatus.PENDING)
                .validity(OrderValidity.DAY)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        order2 = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .userId(USER_ID_1)
                .symbol("RELIANCE")
                .side(OrderSide.SELL)
                .orderType(OrderType.MARKET)
                .quantity(new BigDecimal("5"))
                .status(OrderStatus.FILLED)
                .validity(OrderValidity.DAY)
                .filledQuantity(new BigDecimal("5"))
                .build();

        order3 = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .userId(USER_ID_2)
                .symbol("TCS")
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(new BigDecimal("20"))
                .price(new BigDecimal("3500.00"))
                .status(OrderStatus.PENDING)
                .validity(OrderValidity.DAY)
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();
    }

    @Test
    void findByOrderId_WhenExists_ShouldReturnOrder() {
        Optional<Order> found = orderRepository.findByOrderId(order1.getOrderId());

        assertTrue(found.isPresent());
        assertEquals(order1.getOrderId(), found.get().getOrderId());
        assertEquals("AAPL", found.get().getSymbol());
    }

    @Test
    void findByOrderId_WhenNotExists_ShouldReturnEmpty() {
        Optional<Order> found = orderRepository.findByOrderId("non-existent-id");

        assertTrue(found.isEmpty());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_ShouldReturnPagedOrders() {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(USER_ID_1, PageRequest.of(0, 10));

        assertEquals(2, orders.getTotalElements());
        assertEquals(2, orders.getContent().size());
    }

    @Test
    void findByUserIdAndStatusIn_ShouldReturnMatchingOrders() {
        List<Order> orders = orderRepository.findByUserIdAndStatusIn(
                USER_ID_1, Arrays.asList(OrderStatus.PENDING, OrderStatus.FILLED));

        assertEquals(2, orders.size());
    }

    @Test
    void findActiveOrdersByUserId_ShouldReturnNonTerminalOrders() {
        List<OrderStatus> terminalStatuses = Arrays.asList(
                OrderStatus.FILLED, OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.EXPIRED);

        List<Order> activeOrders = orderRepository.findActiveOrdersByUserId(USER_ID_1, terminalStatuses);

        assertEquals(1, activeOrders.size());
        assertEquals(OrderStatus.PENDING, activeOrders.get(0).getStatus());
    }

    @Test
    void findExpiredOrders_ShouldReturnExpiredPendingOrders() {
        List<OrderStatus> activeStatuses = Arrays.asList(OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED);
        Instant now = Instant.now();

        List<Order> expiredOrders = orderRepository.findExpiredOrders(activeStatuses, now);

        assertEquals(1, expiredOrders.size());
        assertEquals("TCS", expiredOrders.get(0).getSymbol());
    }

    @Test
    void findBySymbolAndStatusIn_ShouldReturnMatchingOrders() {
        List<Order> orders = orderRepository.findBySymbolAndStatusIn(
                "AAPL", Arrays.asList(OrderStatus.PENDING));

        assertEquals(1, orders.size());
        assertEquals("AAPL", orders.get(0).getSymbol());
    }

    @Test
    void countByUserIdAndStatus_ShouldReturnCorrectCount() {
        long count = orderRepository.countByUserIdAndStatus(USER_ID_1, OrderStatus.PENDING);

        assertEquals(1, count);
    }

    @Test
    void findActiveOrdersByUserIdAndSymbol_ShouldReturnMatchingOrders() {
        List<OrderStatus> terminalStatuses = Arrays.asList(
                OrderStatus.FILLED, OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.EXPIRED);

        List<Order> orders = orderRepository.findActiveOrdersByUserIdAndSymbol(
                USER_ID_1, "AAPL", terminalStatuses);

        assertEquals(1, orders.size());
        assertEquals("AAPL", orders.get(0).getSymbol());
    }

    @Test
    void save_ShouldPersistOrder() {
        Order newOrder = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .userId(3L)
                .symbol("INFY")
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(new BigDecimal("15"))
                .price(new BigDecimal("1800.00"))
                .status(OrderStatus.NEW)
                .validity(OrderValidity.DAY)
                .build();

        Order saved = orderRepository.save(newOrder);

        assertNotNull(saved.getId());
        assertEquals("INFY", saved.getSymbol());
    }
}
