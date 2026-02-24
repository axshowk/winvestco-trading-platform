package in.winvestco.trade_service.service;

import in.winvestco.trade_service.dto.CreateTradeRequest;
import in.winvestco.trade_service.exception.TradeValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Service for validating trade business rules.
 * 
 * Validates:
 * - Market hours
 * - Order value limits
 * - Quantity limits
 * - Price validity
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TradeValidationService {

    private final MarketQuoteProjectionService marketQuoteProjectionService;

    @Value("${trading.market-open-hour:9}")
    private int marketOpenHour;

    @Value("${trading.market-open-minute:15}")
    private int marketOpenMinute;

    @Value("${trading.market-close-hour:15}")
    private int marketCloseHour;

    @Value("${trading.market-close-minute:30}")
    private int marketCloseMinute;

    @Value("${trading.timezone:Asia/Kolkata}")
    private String timezone;

    @Value("${trading.max-order-value:10000000}")
    private BigDecimal maxOrderValue;

    @Value("${trading.min-order-value:100}")
    private BigDecimal minOrderValue;

    @Value("${trading.max-quantity-per-order:100000}")
    private BigDecimal maxQuantityPerOrder;

    @Value("${trading.quote.staleness-sla-seconds:30}")
    private long quoteStalenessSlaSeconds;

    /**
     * Validate a trade request.
     * 
     * @param request the trade request to validate
     * @throws TradeValidationException if validation fails
     */
    public void validate(CreateTradeRequest request) {
        log.debug("Validating trade request for order: {}", request.getOrderId());

        validateSymbol(request.getSymbol());
        validateQuoteFreshness(request.getSymbol());
        validateQuantity(request.getQuantity());
        validatePrice(request.getPrice(), request.getQuantity());
        // Market hours validation is optional - can be enabled for paper trading
        // validateMarketHours();

        log.debug("Trade request validated successfully for order: {}", request.getOrderId());
    }

    /**
     * Validate symbol format.
     */
    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new TradeValidationException("Symbol is required", "INVALID_SYMBOL");
        }
        
        // Symbol should be uppercase alphanumeric
        if (!symbol.matches("^[A-Z0-9]+$")) {
            throw new TradeValidationException("Invalid symbol format: " + symbol, "INVALID_SYMBOL");
        }
    }

    /**
     * Validate quantity.
     */
    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TradeValidationException("Quantity must be positive", "INVALID_QUANTITY");
        }

        if (quantity.compareTo(maxQuantityPerOrder) > 0) {
            throw new TradeValidationException(
                    String.format("Quantity %s exceeds maximum allowed %s", quantity, maxQuantityPerOrder),
                    "QUANTITY_EXCEEDED");
        }
    }

    private void validateQuoteFreshness(String symbol) {
        MarketQuoteProjectionService.QuoteSnapshot snapshot = marketQuoteProjectionService.getQuote(symbol)
                .orElseThrow(() -> new TradeValidationException(
                        "Cannot place trade without a market quote for symbol: " + symbol,
                        "QUOTE_UNAVAILABLE"));

        long quoteEpochSeconds = snapshot.eventTimeSeconds() > 0
                ? snapshot.eventTimeSeconds()
                : snapshot.projectionUpdatedAtMs() / 1000;

        Duration quoteAge = Duration.between(Instant.ofEpochSecond(quoteEpochSeconds), Instant.now());
        if (quoteAge.isNegative()) {
            quoteAge = Duration.ZERO;
        }

        Duration allowedAge = Duration.ofSeconds(quoteStalenessSlaSeconds);
        if (quoteAge.compareTo(allowedAge) > 0) {
            throw new TradeValidationException(String.format(
                    "Quote for symbol %s is stale (age=%ss, sla=%ss)",
                    symbol,
                    quoteAge.getSeconds(),
                    allowedAge.getSeconds()), "STALE_QUOTE");
        }
    }

    /**
     * Validate price and order value.
     */
    private void validatePrice(BigDecimal price, BigDecimal quantity) {
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new TradeValidationException("Price must be positive", "INVALID_PRICE");
            }

            BigDecimal orderValue = price.multiply(quantity);
            
            if (orderValue.compareTo(minOrderValue) < 0) {
                throw new TradeValidationException(
                        String.format("Order value %s is below minimum %s", orderValue, minOrderValue),
                        "ORDER_VALUE_TOO_LOW");
            }

            if (orderValue.compareTo(maxOrderValue) > 0) {
                throw new TradeValidationException(
                        String.format("Order value %s exceeds maximum %s", orderValue, maxOrderValue),
                        "ORDER_VALUE_EXCEEDED");
            }
        }
    }

    /**
     * Validate market hours (optional - for paper trading).
     */
    public void validateMarketHours() {
        ZoneId zone = ZoneId.of(timezone);
        LocalDateTime now = LocalDateTime.now(zone);
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        // Check if it's a weekend
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            throw new TradeValidationException("Market is closed on weekends", "MARKET_CLOSED");
        }

        LocalTime currentTime = now.toLocalTime();
        LocalTime openTime = LocalTime.of(marketOpenHour, marketOpenMinute);
        LocalTime closeTime = LocalTime.of(marketCloseHour, marketCloseMinute);

        if (currentTime.isBefore(openTime) || currentTime.isAfter(closeTime)) {
            throw new TradeValidationException(
                    String.format("Market is closed. Trading hours: %s - %s IST", openTime, closeTime),
                    "MARKET_CLOSED");
        }
    }

    /**
     * Check if market is currently open.
     */
    public boolean isMarketOpen() {
        try {
            validateMarketHours();
            return true;
        } catch (TradeValidationException e) {
            return false;
        }
    }
}
