package in.winvestco.marketservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.marketservice.dto.MarketDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MarketDataService
 * Uses Mockito to mock Redis interactions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService Tests")
class MarketDataServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private MarketDataService marketDataService;
    private ObjectMapper objectMapper;

    private static final String TEST_SYMBOL = "RELIANCE";
    private static final String MARKET_DATA_KEY_PREFIX = "market:data:";

    @BeforeEach
    void setUp() {
        marketDataService = new MarketDataService(redisTemplate);
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(marketDataService, "objectMapper", objectMapper);
    }

    @Nested
    @DisplayName("Save Market Data Tests")
    class SaveMarketDataTests {

        @Test
        @DisplayName("Should save data to Redis with correct key and TTL")
        void saveMarketData_ShouldSaveToRedis() {
            String testData = "{\"symbol\":\"RELIANCE\",\"price\":2500.50}";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            marketDataService.saveMarketData(TEST_SYMBOL, testData);

            verify(redisTemplate).opsForValue();
            verify(valueOperations).set(
                    eq(MARKET_DATA_KEY_PREFIX + TEST_SYMBOL),
                    eq(testData),
                    any(Duration.class));
        }

        @Test
        @DisplayName("Should serialize DTO and save to Redis")
        void saveMarketDataDTO_ShouldSerializeAndSave() throws JsonProcessingException {
            MarketDataDTO dto = new MarketDataDTO();
            dto.setSymbol(TEST_SYMBOL);
            dto.setOpen(BigDecimal.valueOf(2450.0));
            dto.setHigh(BigDecimal.valueOf(2520.0));
            dto.setLow(BigDecimal.valueOf(2440.0));
            dto.setLastTradedPrice(BigDecimal.valueOf(2500.50));

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            marketDataService.saveMarketDataDTO(TEST_SYMBOL, dto);

            verify(valueOperations).set(
                    eq(MARKET_DATA_KEY_PREFIX + TEST_SYMBOL),
                    anyString(),
                    any(Duration.class));
        }
    }

    @Nested
    @DisplayName("Get Market Data Tests")
    class GetMarketDataTests {

        @Test
        @DisplayName("Should retrieve data from Redis")
        void getMarketData_ShouldRetrieveFromRedis() {
            String expectedData = "{\"symbol\":\"RELIANCE\",\"price\":2500.50}";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(MARKET_DATA_KEY_PREFIX + TEST_SYMBOL)).thenReturn(expectedData);

            String result = marketDataService.getMarketData(TEST_SYMBOL);

            assertThat(result).isEqualTo(expectedData);
            verify(valueOperations).get(MARKET_DATA_KEY_PREFIX + TEST_SYMBOL);
        }

        @Test
        @DisplayName("Should return null when data not found in Redis")
        void getMarketData_WhenNotFound_ShouldReturnNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(MARKET_DATA_KEY_PREFIX + TEST_SYMBOL)).thenReturn(null);

            String result = marketDataService.getMarketData(TEST_SYMBOL);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should parse JSON to DTO correctly")
        void getMarketDataAsDTO_ShouldParseCorrectly() throws JsonProcessingException {
            MarketDataDTO dto = new MarketDataDTO();
            dto.setSymbol(TEST_SYMBOL);
            dto.setOpen(BigDecimal.valueOf(2450.0));
            dto.setHigh(BigDecimal.valueOf(2520.0));
            dto.setLow(BigDecimal.valueOf(2440.0));
            dto.setLastTradedPrice(BigDecimal.valueOf(2500.50));

            String jsonData = objectMapper.writeValueAsString(dto);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(MARKET_DATA_KEY_PREFIX + TEST_SYMBOL)).thenReturn(jsonData);

            MarketDataDTO result = marketDataService.getMarketDataAsDTO(TEST_SYMBOL);

            assertThat(result).isNotNull();
            assertThat(result.getSymbol()).isEqualTo(TEST_SYMBOL);
            assertThat(result.getLastTradedPrice()).isEqualTo(BigDecimal.valueOf(2500.50));
        }

        @Test
        @DisplayName("Should return null when JSON parsing fails")
        void getMarketDataAsDTO_WhenParseError_ShouldReturnNull() {
            String invalidJson = "not valid json";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(MARKET_DATA_KEY_PREFIX + TEST_SYMBOL)).thenReturn(invalidJson);

            MarketDataDTO result = marketDataService.getMarketDataAsDTO(TEST_SYMBOL);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null when data is null")
        void getMarketDataAsDTO_WhenDataNull_ShouldReturnNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(MARKET_DATA_KEY_PREFIX + TEST_SYMBOL)).thenReturn(null);

            MarketDataDTO result = marketDataService.getMarketDataAsDTO(TEST_SYMBOL);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Get All Stocks Tests")
    class GetAllStocksTests {

        @Test
        @DisplayName("Should return null when no index data available")
        void getAllStocks_WhenNoData_ShouldReturnEmptyResult() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            String result = marketDataService.getAllStocks();

            // Even with no data, it should return a valid JSON with empty data
            assertThat(result).isNotNull();
            assertThat(result).contains("\"totalCount\":0");
        }

        @Test
        @DisplayName("Should aggregate stocks from multiple indices")
        void getAllStocks_ShouldAggregateFromIndices() throws JsonProcessingException {
            String indexData = "{\"data\":[{\"symbol\":\"RELIANCE\",\"ltP\":2500},{\"symbol\":\"TCS\",\"ltP\":3500}]}";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            // Return data for NIFTY 50, null for others
            when(valueOperations.get(MARKET_DATA_KEY_PREFIX + "NIFTY 50")).thenReturn(indexData);
            when(valueOperations.get(argThat(key -> key != null && !key.equals(MARKET_DATA_KEY_PREFIX + "NIFTY 50"))))
                    .thenReturn(null);

            String result = marketDataService.getAllStocks();

            assertThat(result).isNotNull();
            assertThat(result).contains("\"totalCount\":2");
            assertThat(result).contains("RELIANCE");
            assertThat(result).contains("TCS");
        }

        @Test
        @DisplayName("Should deduplicate stocks by symbol")
        void getAllStocks_ShouldDeduplicateBySymbol() {
            // Same stock in two different indices
            String nifty50Data = "{\"data\":[{\"symbol\":\"RELIANCE\",\"ltP\":2500}]}";
            String bankNiftyData = "{\"data\":[{\"symbol\":\"RELIANCE\",\"ltP\":2500},{\"symbol\":\"HDFCBANK\",\"ltP\":1600}]}";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(MARKET_DATA_KEY_PREFIX + "NIFTY 50")).thenReturn(nifty50Data);
            when(valueOperations.get(MARKET_DATA_KEY_PREFIX + "NIFTY BANK")).thenReturn(bankNiftyData);
            when(valueOperations.get(argThat(key -> key != null &&
                    !key.equals(MARKET_DATA_KEY_PREFIX + "NIFTY 50") &&
                    !key.equals(MARKET_DATA_KEY_PREFIX + "NIFTY BANK"))))
                    .thenReturn(null);

            String result = marketDataService.getAllStocks();

            assertThat(result).isNotNull();
            // Should have 2 unique stocks, not 3
            assertThat(result).contains("\"totalCount\":2");
        }
    }

    @Nested
    @DisplayName("Get Stock Quote Tests")
    class GetStockQuoteTests {

        @Test
        @DisplayName("Should find stock quote in an index")
        void getStockQuote_ShouldFindInIndex() throws JsonProcessingException {
            String indexData = "{\"data\":[{\"symbol\":\"RELIANCE\",\"ltP\":2500}]}";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(MARKET_DATA_KEY_PREFIX + "NIFTY 50")).thenReturn(indexData);
            when(valueOperations.get(argThat(key -> key != null && !key.equals(MARKET_DATA_KEY_PREFIX + "NIFTY 50"))))
                    .thenReturn(null);

            String result = marketDataService.getStockQuote("RELIANCE");

            assertThat(result).isNotNull();
            assertThat(result).contains("RELIANCE");
        }

        @Test
        @DisplayName("Should return null when symbol not found")
        void getStockQuote_WhenNotFound_ShouldReturnNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            String result = marketDataService.getStockQuote("NONEXISTENT");

            assertThat(result).isNull();
        }
    }
}
