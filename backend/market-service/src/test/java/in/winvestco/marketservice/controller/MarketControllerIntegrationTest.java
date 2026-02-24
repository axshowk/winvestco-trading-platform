package in.winvestco.marketservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.marketservice.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketController.class)
class MarketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketDataService marketDataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getIndexData_WhenDataExists_ReturnsOk() throws Exception {
        // Arrange
        String symbol = "NIFTY 50";
        String mockData = "{\"symbol\":\"NIFTY 50\",\"lastPrice\":19500.50}";
        when(marketDataService.getMarketData(symbol)).thenReturn(mockData);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/indices/{symbol}", symbol))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().json(mockData));
    }

    @Test
    void getIndexData_WhenDataNotExists_ReturnsNotFound() throws Exception {
        // Arrange
        String symbol = "UNKNOWN";
        when(marketDataService.getMarketData(symbol)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/indices/{symbol}", symbol))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllStocks_WhenDataExists_ReturnsOk() throws Exception {
        // Arrange
        String mockData = "{\"data\":[{\"symbol\":\"RELIANCE\",\"lastPrice\":2500.75}],\"totalCount\":1}";
        when(marketDataService.getAllStocks()).thenReturn(mockData);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/stocks/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().json(mockData));
    }

    @Test
    void getAllStocks_WhenDataNotExists_ReturnsNoContent() throws Exception {
        // Arrange
        when(marketDataService.getAllStocks()).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/stocks/all"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getStockQuote_WhenDataExists_ReturnsOk() throws Exception {
        // Arrange
        String symbol = "RELIANCE";
        String mockData = "{\"symbol\":\"RELIANCE\",\"lastPrice\":2500.75,\"change\":1.5}";
        when(marketDataService.getStockQuote(symbol)).thenReturn(mockData);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/stocks/{symbol}", symbol))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().json(mockData));
    }

    @Test
    void getStockQuote_WhenDataNotExists_ReturnsNotFound() throws Exception {
        // Arrange
        String symbol = "UNKNOWN";
        when(marketDataService.getStockQuote(symbol)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/stocks/{symbol}", symbol))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIndexData_WithInvalidSymbol_ReturnsNotFound() throws Exception {
        // Arrange
        String invalidSymbol = "";
        when(marketDataService.getMarketData(anyString())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/indices/{symbol}", invalidSymbol))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStockQuote_WithInvalidSymbol_ReturnsNotFound() throws Exception {
        // Arrange
        String invalidSymbol = "";
        when(marketDataService.getStockQuote(anyString())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/stocks/{symbol}", invalidSymbol))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIndexData_WithSpecialCharacters_ReturnsOk() throws Exception {
        // Arrange
        String symbol = "NIFTY BANK";
        String mockData = "{\"symbol\":\"NIFTY BANK\",\"lastPrice\":45000.25}";
        when(marketDataService.getMarketData(symbol)).thenReturn(mockData);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/indices/{symbol}", symbol))
                .andExpect(status().isOk())
                .andExpect(content().json(mockData));
    }

    @Test
    void getStockQuote_WithLowerCaseSymbol_ReturnsOk() throws Exception {
        // Arrange
        String symbol = "reliance";
        String mockData = "{\"symbol\":\"RELIANCE\",\"lastPrice\":2500.75}";
        when(marketDataService.getStockQuote(symbol)).thenReturn(mockData);

        // Act & Assert
        mockMvc.perform(get("/api/v1/market/stocks/{symbol}", symbol))
                .andExpect(status().isOk())
                .andExpect(content().json(mockData));
    }
}
