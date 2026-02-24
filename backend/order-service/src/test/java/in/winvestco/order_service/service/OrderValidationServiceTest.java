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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderValidationServiceTest {

    @Mock
    private MarketServiceClient marketServiceClient;

    @Mock
    private MarketQuoteProjectionService marketQuoteProjectionService;

    @InjectMocks
    private OrderValidationService validationService;

    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateOrderRequest();
        request.setSymbol("AAPL");
        request.setOrderType(OrderType.MARKET);
        request.setQuantity(new BigDecimal("10"));

        ReflectionTestUtils.setField(validationService, "quoteStalenessSlaSeconds", 30L);
        when(marketQuoteProjectionService.getQuote("AAPL")).thenReturn(Optional.of(freshQuote("AAPL")));
    }

    @Test
    void validate_ValidMarketOrder_ShouldPass() {
        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    void validate_InvalidSymbol_ShouldThrowException() {
        request.setSymbol("INVALID");
        when(marketQuoteProjectionService.getQuote("INVALID")).thenReturn(Optional.empty());
        when(marketServiceClient.symbolExists("INVALID")).thenReturn(false);

        OrderValidationException exception = assertThrows(OrderValidationException.class,
                () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Invalid symbol"));
    }

    @Test
    void validate_MarketServiceFailure_ShouldPassWhenFreshQuoteExists() {
        when(marketServiceClient.symbolExists(anyString())).thenThrow(new RuntimeException("Service Down"));
        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    void validate_LimitOrder_WithValidPrice_ShouldPass() {
        request.setOrderType(OrderType.LIMIT);
        request.setPrice(new BigDecimal("150.00"));

        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    void validate_LimitOrder_MissingPrice_ShouldThrowException() {
        request.setOrderType(OrderType.LIMIT);
        request.setPrice(null);

        OrderValidationException exception = assertThrows(OrderValidationException.class,
                () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Price is required"));
    }

    @Test
    void validate_LimitOrder_NegativePrice_ShouldThrowException() {
        request.setOrderType(OrderType.LIMIT);
        request.setPrice(new BigDecimal("-10.00"));

        OrderValidationException exception = assertThrows(OrderValidationException.class,
                () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Price must be positive"));
    }

    @Test
    void validate_StopLimitOrder_WithValidPrices_ShouldPass() {
        request.setOrderType(OrderType.STOP_LIMIT);
        request.setPrice(new BigDecimal("150.00"));
        request.setStopPrice(new BigDecimal("145.00"));

        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    void validate_StopLossOrder_MissingStopPrice_ShouldThrowException() {
        request.setOrderType(OrderType.STOP_LOSS);
        request.setStopPrice(null);

        OrderValidationException exception = assertThrows(OrderValidationException.class,
                () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Stop price is required"));
    }

    @Test
    void validate_StopLossOrder_NegativeStopPrice_ShouldThrowException() {
        request.setOrderType(OrderType.STOP_LOSS);
        request.setStopPrice(new BigDecimal("-5.00"));

        OrderValidationException exception = assertThrows(OrderValidationException.class,
                () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("Stop price must be positive"));
    }

    @Test
    void validate_StaleQuote_ShouldThrowException() {
        request.setSymbol("MSFT");
        when(marketQuoteProjectionService.getQuote("MSFT")).thenReturn(Optional.of(staleQuote("MSFT")));

        OrderValidationException exception = assertThrows(OrderValidationException.class,
                () -> validationService.validate(request));
        assertTrue(exception.getMessage().contains("stale"));
    }

    private MarketQuoteProjectionService.QuoteSnapshot freshQuote(String symbol) {
        long nowSeconds = Instant.now().getEpochSecond();
        long nowMillis = Instant.now().toEpochMilli();
        return new MarketQuoteProjectionService.QuoteSnapshot(
                symbol,
                "NSE",
                100.0,
                99.0,
                101.0,
                98.0,
                99.5,
                0.5,
                0.5,
                1000L,
                nowSeconds,
                nowMillis);
    }

    private MarketQuoteProjectionService.QuoteSnapshot staleQuote(String symbol) {
        Instant staleTs = Instant.now().minusSeconds(300);
        return new MarketQuoteProjectionService.QuoteSnapshot(
                symbol,
                "NSE",
                100.0,
                99.0,
                101.0,
                98.0,
                99.5,
                0.5,
                0.5,
                1000L,
                staleTs.getEpochSecond(),
                staleTs.toEpochMilli());
    }
}
