package in.winvestco.trade_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for market-service to fetch current market prices.
 * Used by MockExecutionEngine for realistic trade execution pricing.
 */
@FeignClient(name = "market-service", fallback = MarketServiceClientFallback.class)
public interface MarketServiceClient {

    /**
     * Get stock quote data including current price.
     * Returns JSON string with lastPrice, open, high, low, etc.
     */
    @GetMapping("/api/v1/market/stocks/{symbol}")
    String getStockQuote(@PathVariable("symbol") String symbol);
}
