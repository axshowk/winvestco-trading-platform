package in.winvestco.trade_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback for MarketServiceClient when market-service is unavailable.
 * Returns null to signal that execution engine should use order price.
 */
@Component
@Slf4j
public class MarketServiceClientFallback implements MarketServiceClient {

    @Override
    public String getStockQuote(String symbol) {
        log.warn("Market service unavailable, returning null quote for: {}", symbol);
        return null;
    }
}
