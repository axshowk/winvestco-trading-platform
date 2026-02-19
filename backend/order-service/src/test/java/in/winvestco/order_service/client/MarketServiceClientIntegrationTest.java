package in.winvestco.order_service.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MarketServiceClientIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MarketServiceClient marketServiceClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        registry.add("market-service.ribbon.listOfServers", () -> "localhost:" + wireMockServer.port());
        registry.add("feign.client.config.market-service.url", () -> "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
    }

    @Test
    void symbolExists_WhenSymbolExists_ShouldReturnTrue() {
        wireMockServer.stubFor(get("/api/v1/market/stocks/AAPL/exists")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        Boolean result = marketServiceClient.symbolExists("AAPL");

        assertTrue(result);
        wireMockServer.verify(getRequestedFor(urlEqualTo("/api/v1/market/stocks/AAPL/exists")));
    }

    @Test
    void symbolExists_WhenSymbolNotExists_ShouldReturnFalse() {
        wireMockServer.stubFor(get("/api/v1/market/stocks/INVALID/exists")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("false")));

        Boolean result = marketServiceClient.symbolExists("INVALID");

        assertFalse(result);
    }

    @Test
    void symbolExists_WhenServiceUnavailable_ShouldFallback() {
        wireMockServer.stubFor(get("/api/v1/market/stocks/RELIANCE/exists")
                .willReturn(aResponse()
                        .withStatus(503)));

        Boolean result = marketServiceClient.symbolExists("RELIANCE");

        assertTrue(result);
    }

    @Test
    void getMarketPrice_ShouldReturnPriceResponse() {
        String jsonResponse = """
                {
                    "symbol": "AAPL",
                    "lastPrice": 150.50,
                    "bidPrice": 150.45,
                    "askPrice": 150.55
                }
                """;

        wireMockServer.stubFor(get("/api/v1/market/stocks/AAPL/price")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        MarketServiceClient.MarketPriceResponse response = marketServiceClient.getMarketPrice("AAPL");

        assertNotNull(response);
        assertEquals("AAPL", response.symbol());
        assertEquals(new BigDecimal("150.50"), response.lastPrice());
        assertEquals(new BigDecimal("150.45"), response.bidPrice());
        assertEquals(new BigDecimal("150.55"), response.askPrice());
    }

    @Test
    void getMarketPrice_WhenServiceUnavailable_ShouldFallback() {
        wireMockServer.stubFor(get("/api/v1/market/stocks/TCS/price")
                .willReturn(aResponse()
                        .withStatus(503)));

        MarketServiceClient.MarketPriceResponse response = marketServiceClient.getMarketPrice("TCS");

        assertNotNull(response);
        assertEquals("TCS", response.symbol());
        assertNull(response.lastPrice());
    }
}
