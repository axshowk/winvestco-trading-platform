package in.winvestco.order_service;

import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderStatus;
import in.winvestco.common.enums.OrderType;
import in.winvestco.common.enums.OrderValidity;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.dto.OrderDTO;
import in.winvestco.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceApplicationIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    void contextLoads() {
        assertNotNull(orderService);
    }

    @Test
    void createOrder_ShouldCreateOrderInDatabase() {
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSymbol("AAPL");
        request.setSide(OrderSide.BUY);
        request.setOrderType(OrderType.LIMIT);
        request.setQuantity(new BigDecimal("10"));
        request.setPrice(new BigDecimal("150.00"));
        request.setValidity(OrderValidity.DAY);

        OrderDTO result = orderService.createOrder(userId, request);

        assertNotNull(result);
        assertNotNull(result.getOrderId());
        assertEquals("AAPL", result.getSymbol());
        assertEquals(OrderStatus.NEW, result.getStatus());
    }

    @Test
    void createAndGetOrder_ShouldReturnOrder() {
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSymbol("RELIANCE");
        request.setSide(OrderSide.SELL);
        request.setOrderType(OrderType.MARKET);
        request.setQuantity(new BigDecimal("5"));
        request.setValidity(OrderValidity.DAY);

        OrderDTO created = orderService.createOrder(userId, request);
        OrderDTO retrieved = orderService.getOrder(created.getOrderId());

        assertNotNull(retrieved);
        assertEquals(created.getOrderId(), retrieved.getOrderId());
        assertEquals("RELIANCE", retrieved.getSymbol());
    }
}
