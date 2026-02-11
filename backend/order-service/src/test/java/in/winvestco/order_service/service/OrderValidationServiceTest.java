package in.winvestco.order_service.service;

import in.winvestco.common.enums.OrderType;
import in.winvestco.order_service.client.MarketServiceClient;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.exception.OrderValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderValidationServiceTest {

    @Mock
    private MarketServiceClient marketServiceClient;

    @InjectMocks
    private OrderValidationService validationService;

    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateOrderRequest();
        request.setSymbol("AAPL");
        request.setOrderType(OrderType.MARKET);
        request.setQuantity(new BigDecimal("10"));
    }

    @Test
    void validate_ValidMarketOrder_ShouldPass() {
        when(marketServiceClient.symbolExists("AAPL")).thenReturn(true);
        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    void validate_InvalidSymbol_ShouldThrowException() {
        when(marketServiceClient.symbolExists("INVALID")).thenReturn(false);
        request.setSymbol("INVALID");
        
        OrderValidationException exception = assertThrows(OrderValidationException.class, 
            () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Invalid symbol"));
    }

    @Test
    void validate_MarketServiceFailure_ShouldLogAndPass() {
        when(marketServiceClient.symbolExists(anyString())).thenThrow(new RuntimeException("Service Down"));
        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    void validate_LimitOrder_WithValidPrice_ShouldPass() {
        when(marketServiceClient.symbolExists("AAPL")).thenReturn(true);
        request.setOrderType(OrderType.LIMIT);
        request.setPrice(new BigDecimal("150.00"));
        
        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    void validate_LimitOrder_MissingPrice_ShouldThrowException() {
        when(marketServiceClient.symbolExists("AAPL")).thenReturn(true);
        request.setOrderType(OrderType.LIMIT);
        request.setPrice(null);
        
        OrderValidationException exception = assertThrows(OrderValidationException.class, 
            () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Price is required"));
    }

    @Test
    void validate_LimitOrder_NegativePrice_ShouldThrowException() {
        when(marketServiceClient.symbolExists("AAPL")).thenReturn(true);
        request.setOrderType(OrderType.LIMIT);
        request.setPrice(new BigDecimal("-10.00"));
        
        OrderValidationException exception = assertThrows(OrderValidationException.class, 
            () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Price must be positive"));
    }

    @Test
    void validate_StopLimitOrder_WithValidPrices_ShouldPass() {
        when(marketServiceClient.symbolExists("AAPL")).thenReturn(true);
        request.setOrderType(OrderType.STOP_LIMIT);
        request.setPrice(new BigDecimal("150.00"));
        request.setStopPrice(new BigDecimal("145.00"));
        
        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    void validate_StopLossOrder_MissingStopPrice_ShouldThrowException() {
        when(marketServiceClient.symbolExists("AAPL")).thenReturn(true);
        request.setOrderType(OrderType.STOP_LOSS);
        request.setStopPrice(null);
        
        OrderValidationException exception = assertThrows(OrderValidationException.class, 
            () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Stop price is required"));
    }

    @Test
    void validate_StopLossOrder_NegativeStopPrice_ShouldThrowException() {
        when(marketServiceClient.symbolExists("AAPL")).thenReturn(true);
        request.setOrderType(OrderType.STOP_LOSS);
        request.setStopPrice(new BigDecimal("-5.00"));
        
        OrderValidationException exception = assertThrows(OrderValidationException.class, 
            () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Stop price must be positive"));
    }
}
