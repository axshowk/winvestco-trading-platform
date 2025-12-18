package in.winvestco.order_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for MarketServiceClient when market-service is unavailable
 */
@Component
@Slf4j
public class MarketServiceClientFallback implements MarketServiceClient {

    @Override
    public Boolean symbolExists(String symbol) {
        log.warn("Market service unavailable, defaulting to symbol exists: {}", symbol);
        // In fallback, we allow the order but log a warning
        return true;
    }

    @Override
    public MarketPriceResponse getMarketPrice(String symbol) {
        log.warn("Market service unavailable, returning null price for: {}", symbol);
        return new MarketPriceResponse(symbol, null, null, null);
    }
}
