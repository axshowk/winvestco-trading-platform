package in.winvestco.portfolio_service.client;

import in.winvestco.portfolio_service.dto.StockQuoteDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fallback implementation for Market Service client.
 * Returns empty or null values when Market Service is unavailable.
 */
@Component
public class MarketServiceFallback implements MarketServiceClient {

    @Override
    public StockQuoteDTO getStockQuote(String symbol) {
        return StockQuoteDTO.builder()
                .symbol(symbol)
                .lastPrice(null)
                .build();
    }

    @Override
    public List<StockQuoteDTO> getBulkQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }
        return symbols.stream()
                .map(this::getStockQuote)
                .collect(Collectors.toList());
    }
}
