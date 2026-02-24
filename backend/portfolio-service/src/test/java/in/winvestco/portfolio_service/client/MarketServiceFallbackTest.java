package in.winvestco.portfolio_service.client;

import in.winvestco.portfolio_service.dto.StockQuoteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarketServiceFallbackTest {

    private MarketServiceFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new MarketServiceFallback();
    }

    @Test
    @DisplayName("getStockQuote should return DTO with null lastPrice")
    void getStockQuote_shouldReturnDtoWithNullPrice() {
        StockQuoteDTO result = fallback.getStockQuote("RELIANCE");

        assertNotNull(result);
        assertEquals("RELIANCE", result.getSymbol());
        assertNull(result.getLastPrice());
    }

    @Test
    @DisplayName("getBulkQuotes with symbols should return fallback list")
    void getBulkQuotes_withSymbols_shouldReturnFallbackList() {
        List<StockQuoteDTO> result = fallback.getBulkQuotes(List.of("RELIANCE", "TCS"));

        assertEquals(2, result.size());
        assertEquals("RELIANCE", result.get(0).getSymbol());
        assertEquals("TCS", result.get(1).getSymbol());
        assertNull(result.get(0).getLastPrice());
    }

    @Test
    @DisplayName("getBulkQuotes with null should return empty list")
    void getBulkQuotes_withNull_shouldReturnEmpty() {
        List<StockQuoteDTO> result = fallback.getBulkQuotes(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getBulkQuotes with empty list should return empty list")
    void getBulkQuotes_withEmpty_shouldReturnEmpty() {
        List<StockQuoteDTO> result = fallback.getBulkQuotes(Collections.emptyList());
        assertTrue(result.isEmpty());
    }
}
