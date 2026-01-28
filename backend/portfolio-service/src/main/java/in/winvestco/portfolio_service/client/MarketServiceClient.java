package in.winvestco.portfolio_service.client;

import in.winvestco.portfolio_service.dto.StockQuoteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Feign client for communicating with the Market Service.
 */
@FeignClient(name = "market-service", fallback = MarketServiceFallback.class)
public interface MarketServiceClient {

    @GetMapping("/api/v1/market/stocks/{symbol}")
    StockQuoteDTO getStockQuote(@PathVariable("symbol") String symbol);

    @PostMapping("/api/v1/market/stocks/bulk")
    List<StockQuoteDTO> getBulkQuotes(@RequestBody List<String> symbols);
}
