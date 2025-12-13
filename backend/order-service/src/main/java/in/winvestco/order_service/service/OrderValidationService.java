package in.winvestco.order_service.service;

import in.winvestco.common.enums.OrderType;
import in.winvestco.order_service.client.MarketServiceClient;
import in.winvestco.order_service.dto.CreateOrderRequest;
import in.winvestco.order_service.exception.OrderValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for validating orders before processing
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderValidationService {

    private final MarketServiceClient marketServiceClient;

    /**
     * Validate order request
     */
    public void validate(CreateOrderRequest request) {
        validateSymbol(request.getSymbol());
        validatePriceForOrderType(request);
        validateStopPrice(request);
    }

    /**
     * Validate symbol exists in market-service
     */
    private void validateSymbol(String symbol) {
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
