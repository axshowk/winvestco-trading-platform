package in.winvestco.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.enums.OrderSide;
import in.winvestco.common.enums.OrderStatus;
import in.winvestco.common.enums.OrderType;
import in.winvestco.order_service.dto.CancelOrderRequest;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.dto.OrderDTO;
import in.winvestco.order_service.service.OrderService;
import in.winvestco.common.util.LoggingUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

// ...

    @MockBean
    private OrderService orderService;

    @MockBean
    private LoggingUtils loggingUtils;

    @MockBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        orderDTO = new OrderDTO();
        orderDTO.setOrderId("test-order-id");
        orderDTO.setUserId(1L);
        orderDTO.setSymbol("AAPL");
        orderDTO.setSide(OrderSide.BUY);
        orderDTO.setOrderType(OrderType.LIMIT);
        orderDTO.setQuantity(new BigDecimal("10"));
        orderDTO.setPrice(new BigDecimal("150.00"));
        orderDTO.setStatus(OrderStatus.NEW);
    }

    @Test
    void createOrder_WhenValidRequest_ShouldReturnCreated() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSymbol("AAPL");
        request.setSide(OrderSide.BUY);
        request.setOrderType(OrderType.LIMIT);
        request.setQuantity(new BigDecimal("10"));
        request.setPrice(new BigDecimal("150.00"));

        when(orderService.createOrder(eq(1L), any(CreateOrderRequest.class))).thenReturn(orderDTO);

        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().jwt(jwt -> jwt.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("test-order-id"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void getOrder_ShouldReturnOrder() throws Exception {
        when(orderService.getOrder("test-order-id")).thenReturn(orderDTO);

        mockMvc.perform(get("/api/v1/orders/test-order-id")
                        .with(jwt().jwt(jwt -> jwt.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("test-order-id"));
    }

    @Test
    void getOrders_ShouldReturnPageOfOrders() throws Exception {
        Page<OrderDTO> ordersPage = new PageImpl<>(Collections.singletonList(orderDTO));
        when(orderService.getOrdersForUser(eq(1L), any(Pageable.class))).thenReturn(ordersPage);

        mockMvc.perform(get("/api/v1/orders")
                        .with(jwt().jwt(jwt -> jwt.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value("test-order-id"));
    }

    @Test
    void getActiveOrders_ShouldReturnListOfOrders() throws Exception {
        when(orderService.getActiveOrders(1L)).thenReturn(Collections.singletonList(orderDTO));

        mockMvc.perform(get("/api/v1/orders/active")
                        .with(jwt().jwt(jwt -> jwt.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("test-order-id"));
    }

    @Test
    void cancelOrder_ShouldReturnCancelledOrder() throws Exception {
        CancelOrderRequest request = new CancelOrderRequest();
        request.setReason("Changed mind");

        orderDTO.setStatus(OrderStatus.CANCELLED);
        when(orderService.cancelOrder(eq("test-order-id"), eq(1L), eq("Changed mind"))).thenReturn(orderDTO);

        mockMvc.perform(post("/api/v1/orders/test-order-id/cancel")
                        .with(jwt().jwt(jwt -> jwt.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
