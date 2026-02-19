package in.winvestco.order_service.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MarketServiceClientFallbackTest {

    @InjectMocks
    private MarketServiceClientFallback fallback;

    @Test
    void symbolExists_ShouldReturnTrue() {
        Boolean result = fallback.symbolExists("AAPL");

        assertTrue(result);
    }

    @Test
    void symbolExists_WithDifferentSymbol_ShouldReturnTrue() {
        Boolean result = fallback.symbolExists("RELIANCE");

        assertTrue(result);
    }

    @Test
    void getMarketPrice_ShouldReturnResponseWithNullPrices() {
        MarketServiceClient.MarketPriceResponse result = fallback.getMarketPrice("AAPL");

        assertNotNull(result);
        assertEquals("AAPL", result.symbol());
        assertNull(result.lastPrice());
        assertNull(result.bidPrice());
        assertNull(result.askPrice());
    }

    @Test
    void getMarketPrice_WithDifferentSymbol_ShouldReturnResponseWithSymbol() {
        MarketServiceClient.MarketPriceResponse result = fallback.getMarketPrice("TCS");

        assertNotNull(result);
        assertEquals("TCS", result.symbol());
    }
}
