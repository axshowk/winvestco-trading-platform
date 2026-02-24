package in.winvestco.order_service.service;

import in.winvestco.common.enums.OrderType;
import in.winvestco.order_service.client.MarketServiceClient;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.exception.OrderValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for validating orders before processing
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderValidationService {

    private final MarketServiceClient marketServiceClient;
    private final MarketQuoteProjectionService marketQuoteProjectionService;

    @Value("${trading.quote.staleness-sla-seconds:30}")
    private long quoteStalenessSlaSeconds;

    /**
     * Validate order request
     */
    public void validate(CreateOrderRequest request) {
        validateSymbol(request.getSymbol());
        validateQuoteFreshness(request.getSymbol());
        validatePriceForOrderType(request);
        validateStopPrice(request);
    }

    /**
     * Validate symbol exists in market-service
     */
    private void validateSymbol(String symbol) {
        if (marketQuoteProjectionService.getQuote(symbol).isPresent()) {
            return;
        }

        try {
            Boolean exists = marketServiceClient.symbolExists(symbol);
            if (exists == null || !exists) {
                throw new OrderValidationException("Invalid symbol: " + symbol);
            }
        } catch (OrderValidationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to validate symbol against market-service, allowing order: {}", symbol, e);
            // Allow order to proceed when market-service is unavailable
        }
    }

    private void validateQuoteFreshness(String symbol) {
        Duration allowedAge = Duration.ofSeconds(quoteStalenessSlaSeconds);
        var snapshotOptional = marketQuoteProjectionService.getQuote(symbol);

        if (snapshotOptional.isPresent()) {
            MarketQuoteProjectionService.QuoteSnapshot snapshot = snapshotOptional.get();
            long quoteEpochSeconds = snapshot.eventTimeSeconds() > 0
                    ? snapshot.eventTimeSeconds()
                    : snapshot.projectionUpdatedAtMs() / 1000;

            Duration quoteAge = Duration.between(Instant.ofEpochSecond(quoteEpochSeconds), Instant.now());
            if (quoteAge.isNegative()) {
                quoteAge = Duration.ZERO;
            }

            if (quoteAge.compareTo(allowedAge) <= 0) {
                return;
            }

            log.warn("Projection quote for symbol {} is stale (age={}s, sla={}s), falling back to market-service",
                    symbol, quoteAge.getSeconds(), allowedAge.getSeconds());
        }

        try {
            MarketServiceClient.MarketPriceResponse fallbackPrice = marketServiceClient.getMarketPrice(symbol);
            if (fallbackPrice != null && fallbackPrice.lastPrice() != null) {
                return;
            }
        } catch (Exception e) {
            log.warn("Fallback market-service price lookup failed for symbol {}", symbol, e);
        }

        throw new OrderValidationException(String.format(
                "Cannot place order without a fresh quote for symbol %s (sla=%ss)",
                symbol,
                allowedAge.getSeconds()));
    }

    /**
     * Validate price based on order type
     */
    private void validatePriceForOrderType(CreateOrderRequest request) {
        OrderType type = request.getOrderType();

        if (type == OrderType.LIMIT || type == OrderType.STOP_LIMIT) {
            if (request.getPrice() == null) {
                throw new OrderValidationException(
                        "Price is required for " + type + " orders");
            }
            if (request.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new OrderValidationException("Price must be positive");
            }
        }
    }

    /**
     * Validate stop price for stop orders
     */
    private void validateStopPrice(CreateOrderRequest request) {
        OrderType type = request.getOrderType();

        if (type == OrderType.STOP_LOSS || type == OrderType.STOP_LIMIT) {
            if (request.getStopPrice() == null) {
                throw new OrderValidationException(
                        "Stop price is required for " + type + " orders");
            }
            if (request.getStopPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new OrderValidationException("Stop price must be positive");
            }
        }
    }
}
