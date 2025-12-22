package in.winvestco.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for market-service to validate symbols
 */
@FeignClient(name = "market-service", fallback = MarketServiceClientFallback.class)
public interface MarketServiceClient {

    @GetMapping("/api/v1/market/stocks/{symbol}/exists")
    Boolean symbolExists(@PathVariable("symbol") String symbol);

    @GetMapping("/api/v1/market/stocks/{symbol}/price")
    MarketPriceResponse getMarketPrice(@PathVariable("symbol") String symbol);

    record MarketPriceResponse(
            String symbol,
            java.math.BigDecimal lastPrice,
            java.math.BigDecimal bidPrice,
            java.math.BigDecimal askPrice) {
    }
}
