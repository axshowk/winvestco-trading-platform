package in.winvestco.marketservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.marketservice.dto.MarketDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataValidationTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MarketDataService marketDataService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void saveMarketData_WithValidSymbolAndData_SavesToRedis() {
        // Arrange
        String symbol = "NIFTY 50";
        String data = "{\"symbol\":\"NIFTY 50\",\"lastPrice\":19500.50}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        marketDataService.saveMarketData(symbol, data);

        // Assert
        verify(valueOperations).set(eq("market:data:NIFTY 50"), eq(data), eq(Duration.ofMinutes(10)));
    }

    @Test
    void saveMarketData_WithNullSymbol_StillSavesToRedis() {
        // Arrange
        String symbol = null;
        String data = "{\"symbol\":\"NIFTY 50\",\"lastPrice\":19500.50}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        marketDataService.saveMarketData(symbol, data);

        // Assert
        verify(valueOperations).set(eq("market:data:null"), eq(data), eq(Duration.ofMinutes(10)));
    }

    @Test
    void saveMarketData_WithEmptyData_SavesToRedis() {
        // Arrange
        String symbol = "NIFTY 50";
        String data = "";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        marketDataService.saveMarketData(symbol, data);

        // Assert
        verify(valueOperations).set(eq("market:data:NIFTY 50"), eq(""), eq(Duration.ofMinutes(10)));
    }

    @Test
    void getMarketData_WithValidSymbol_ReturnsData() {
        // Arrange
        String symbol = "NIFTY 50";
        String expectedData = "{\"symbol\":\"NIFTY 50\",\"lastPrice\":19500.50}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:data:NIFTY 50")).thenReturn(expectedData);

        // Act
        String result = marketDataService.getMarketData(symbol);

        // Assert
        assertEquals(expectedData, result);
        verify(valueOperations).get("market:data:NIFTY 50");
    }

    @Test
    void getMarketData_WithNonExistentSymbol_ReturnsNull() {
        // Arrange
        String symbol = "UNKNOWN";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:data:UNKNOWN")).thenReturn(null);

        // Act
        String result = marketDataService.getMarketData(symbol);

        // Assert
        assertNull(result);
        verify(valueOperations).get("market:data:UNKNOWN");
    }

    @Test
    void getMarketDataAsDTO_WithValidJSON_ReturnsDTO() throws Exception {
        // Arrange
        String symbol = "NIFTY 50";
        String jsonData = "{\"symbol\":\"NIFTY 50\",\"ltp\":19500.50,\"change\":1.5}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:data:NIFTY 50")).thenReturn(jsonData);

        // Act
        MarketDataDTO result = marketDataService.getMarketDataAsDTO(symbol);

        // Assert
        assertNotNull(result);
        assertEquals("NIFTY 50", result.getSymbol());
        assertEquals(19500.50, result.getLastTradedPrice().doubleValue());
        assertEquals(1.5, result.getChangeValue().doubleValue());
    }

    @Test
    void getMarketDataAsDTO_WithInvalidJSON_ReturnsNull() {
        // Arrange
        String symbol = "NIFTY 50";
        String invalidJson = "{invalid json}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:data:NIFTY 50")).thenReturn(invalidJson);

        // Act
        MarketDataDTO result = marketDataService.getMarketDataAsDTO(symbol);

        // Assert
        assertNull(result);
    }

    @Test
    void getMarketDataAsDTO_WithNullData_ReturnsNull() {
        // Arrange
        String symbol = "NIFTY 50";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:data:NIFTY 50")).thenReturn(null);

        // Act
        MarketDataDTO result = marketDataService.getMarketDataAsDTO(symbol);

        // Assert
        assertNull(result);
    }

    @Test
    void saveMarketDataDTO_WithValidDTO_SavesAsJSON() throws Exception {
        // Arrange
        String symbol = "NIFTY 50";
        MarketDataDTO dto = MarketDataDTO.builder()
                .symbol("NIFTY 50")
                .lastTradedPrice(BigDecimal.valueOf(19500.50))
                .changeValue(BigDecimal.valueOf(1.5))
                .build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        marketDataService.saveMarketDataDTO(symbol, dto);

        // Assert
        verify(valueOperations).set(eq("market:data:NIFTY 50"), anyString(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void saveMarketDataDTO_WithNullDTO_DoesNotThrowException() {
        // Arrange
        String symbol = "NIFTY 50";
        MarketDataDTO dto = null;

        // Act & Assert
        assertDoesNotThrow(() -> marketDataService.saveMarketDataDTO(symbol, dto));
    }

    @Test
    void getAllStocks_WithNoCachedData_ReturnsEmptyResult() {
        // Act
        String result = marketDataService.getAllStocks();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("\"totalCount\":0"));
        assertTrue(result.contains("\"data\":[]"));
    }

    @Test
    void getStockQuote_WithNullSymbol_ReturnsNull() {
        // Act
        String result = marketDataService.getStockQuote(null);

        // Assert
        assertNull(result);
    }

    @Test
    void getStockQuote_WithEmptySymbol_ReturnsNull() {
        // Act
        String result = marketDataService.getStockQuote("");

        // Assert
        assertNull(result);
    }

    @Test
    void getStockQuote_WithValidSymbolNotFound_ReturnsNull() {
        // Arrange
        String symbol = "UNKNOWN";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        String result = marketDataService.getStockQuote(symbol);

        // Assert
        assertNull(result);
    }

    @Test
    void getStockQuote_WithValidSymbolFound_ReturnsData() throws Exception {
        // Arrange
        String symbol = "RELIANCE";
        String indexData = "{\"data\":[{\"symbol\":\"RELIANCE\",\"lastPrice\":2500.75}]}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(contains("NIFTY 50"))).thenReturn(indexData);

        // Act
        String result = marketDataService.getStockQuote(symbol);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("RELIANCE"));
        assertTrue(result.contains("2500.75"));
    }

    @Test
    void getStockQuote_WithMixedCaseSymbol_FindsCaseInsensitive() throws Exception {
        // Arrange
        String symbol = "reliance";
        String indexData = "{\"data\":[{\"symbol\":\"RELIANCE\",\"lastPrice\":2500.75}]}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(contains("NIFTY 50"))).thenReturn(indexData);

        // Act
        String result = marketDataService.getStockQuote(symbol);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("RELIANCE"));
    }

    @Test
    void getStockQuote_FindsNiftySymbol_ReturnsData() throws Exception {
        // Arrange
        String symbol = "NIFTY 50";
        String indexData = "{\"data\":[{\"symbol\":\"NIFTY 50\",\"lastPrice\":19500.50}]}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("market:data:NIFTY 50")).thenReturn(indexData);

        // Act
        String result = marketDataService.getStockQuote(symbol);

        // Debug
        System.out.println("Actual result: " + result);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("NIFTY 50"));
        assertTrue(result.contains("19500.50"));
    }
}
