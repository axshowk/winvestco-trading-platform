package in.winvestco.trade_service.service;

import in.winvestco.trade_service.dto.CreateTradeRequest;
import in.winvestco.trade_service.exception.TradeValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeValidationServiceTest {

    @Mock
    private MarketQuoteProjectionService marketQuoteProjectionService;

    private TradeValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new TradeValidationService(marketQuoteProjectionService);
        ReflectionTestUtils.setField(validationService, "marketOpenHour", 9);
        ReflectionTestUtils.setField(validationService, "marketOpenMinute", 15);
        ReflectionTestUtils.setField(validationService, "marketCloseHour", 15);
        ReflectionTestUtils.setField(validationService, "marketCloseMinute", 30);
        ReflectionTestUtils.setField(validationService, "timezone", "Asia/Kolkata");
        ReflectionTestUtils.setField(validationService, "maxOrderValue", new BigDecimal("10000000"));
        ReflectionTestUtils.setField(validationService, "minOrderValue", new BigDecimal("100"));
        ReflectionTestUtils.setField(validationService, "maxQuantityPerOrder", new BigDecimal("100000"));
        ReflectionTestUtils.setField(validationService, "quoteStalenessSlaSeconds", 30L);

        when(marketQuoteProjectionService.getQuote("RELIANCE")).thenReturn(Optional.of(freshQuote("RELIANCE")));
    }

    @Test
    @DisplayName("validate - valid request should pass")
    void validate_ValidRequest_ShouldPass() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("RELIANCE")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("2500"))
                .orderId("ORD-1")
                .userId(1L)
                .build();

        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    @DisplayName("validate - blank symbol should throw INVALID_SYMBOL")
    void validate_BlankSymbol_ShouldThrowException() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("2500"))
                .build();

        TradeValidationException ex = assertThrows(TradeValidationException.class,
                () -> validationService.validate(request));
        assertEquals("INVALID_SYMBOL", ex.getErrorCode());
    }

    @Test
    @DisplayName("validate - invalid symbol format should throw INVALID_SYMBOL")
    void validate_InvalidSymbolFormat_ShouldThrowException() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("reliance")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("2500"))
                .build();

        TradeValidationException ex = assertThrows(TradeValidationException.class,
                () -> validationService.validate(request));
        assertEquals("INVALID_SYMBOL", ex.getErrorCode());
    }

    @Test
    @DisplayName("validate - zero or negative quantity should throw INVALID_QUANTITY")
    void validate_InvalidQuantity_ShouldThrowException() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("RELIANCE")
                .quantity(BigDecimal.ZERO)
                .price(new BigDecimal("2500"))
                .build();

        TradeValidationException ex = assertThrows(TradeValidationException.class,
                () -> validationService.validate(request));
        assertEquals("INVALID_QUANTITY", ex.getErrorCode());
    }

    @Test
    @DisplayName("validate - quantity exceeding max should throw QUANTITY_EXCEEDED")
    void validate_QuantityExceeded_ShouldThrowException() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("RELIANCE")
                .quantity(new BigDecimal("200000"))
                .price(new BigDecimal("2500"))
                .build();

        TradeValidationException ex = assertThrows(TradeValidationException.class,
                () -> validationService.validate(request));
        assertEquals("QUANTITY_EXCEEDED", ex.getErrorCode());
    }

    @Test
    @DisplayName("validate - negative price should throw INVALID_PRICE")
    void validate_NegativePrice_ShouldThrowException() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("RELIANCE")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("-2500"))
                .build();

        TradeValidationException ex = assertThrows(TradeValidationException.class,
                () -> validationService.validate(request));
        assertEquals("INVALID_PRICE", ex.getErrorCode());
    }

    @Test
    @DisplayName("validate - order value too low should throw ORDER_VALUE_TOO_LOW")
    void validate_OrderValueTooLow_ShouldThrowException() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("RELIANCE")
                .quantity(new BigDecimal("1"))
                .price(new BigDecimal("50"))
                .build();

        TradeValidationException ex = assertThrows(TradeValidationException.class,
                () -> validationService.validate(request));
        assertEquals("ORDER_VALUE_TOO_LOW", ex.getErrorCode());
    }

    @Test
    @DisplayName("validate - order value too high should throw ORDER_VALUE_EXCEEDED")
    void validate_OrderValueExceeded_ShouldThrowException() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("RELIANCE")
                .quantity(new BigDecimal("10000"))
                .price(new BigDecimal("2000"))
                .build();

        TradeValidationException ex = assertThrows(TradeValidationException.class,
                () -> validationService.validate(request));
        assertEquals("ORDER_VALUE_EXCEEDED", ex.getErrorCode());
    }

    @Test
    @DisplayName("validate - null price (market order) should pass price validation")
    void validate_NullPrice_ShouldPass() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("RELIANCE")
                .quantity(new BigDecimal("10"))
                .price(null)
                .orderId("ORD-1")
                .userId(1L)
                .build();

        assertDoesNotThrow(() -> validationService.validate(request));
    }

    @Test
    @DisplayName("validate - missing quote should throw QUOTE_UNAVAILABLE")
    void validate_MissingQuote_ShouldThrowQuoteUnavailable() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("INFY")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("1500"))
                .build();
        when(marketQuoteProjectionService.getQuote("INFY")).thenReturn(Optional.empty());

        TradeValidationException ex = assertThrows(TradeValidationException.class,
                () -> validationService.validate(request));
        assertEquals("QUOTE_UNAVAILABLE", ex.getErrorCode());
    }

    @Test
    @DisplayName("validate - stale quote should throw STALE_QUOTE")
    void validate_StaleQuote_ShouldThrowStaleQuote() {
        CreateTradeRequest request = CreateTradeRequest.builder()
                .symbol("TCS")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("3500"))
                .build();
        when(marketQuoteProjectionService.getQuote("TCS")).thenReturn(Optional.of(staleQuote("TCS")));

        TradeValidationException ex = assertThrows(TradeValidationException.class,
                () -> validationService.validate(request));
        assertEquals("STALE_QUOTE", ex.getErrorCode());
    }

    private MarketQuoteProjectionService.QuoteSnapshot freshQuote(String symbol) {
        Instant now = Instant.now();
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
                now.getEpochSecond(),
                now.toEpochMilli());
    }

    private MarketQuoteProjectionService.QuoteSnapshot staleQuote(String symbol) {
        Instant stale = Instant.now().minusSeconds(300);
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
                stale.getEpochSecond(),
                stale.toEpochMilli());
    }
}
