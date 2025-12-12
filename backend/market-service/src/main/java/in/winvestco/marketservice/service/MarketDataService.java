package in.winvestco.marketservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.marketservice.dto.MarketDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MARKET_DATA_KEY_PREFIX = "market:data:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public void saveMarketData(String symbol, String data) {
        String key = MARKET_DATA_KEY_PREFIX + symbol;
        log.info("Saving market data to Redis for key: {}", key);
        redisTemplate.opsForValue().set(key, data, CACHE_TTL);
    }

    public String getMarketData(String symbol) {
        String key = MARKET_DATA_KEY_PREFIX + symbol;
        return redisTemplate.opsForValue().get(key);
    }

    public MarketDataDTO getMarketDataAsDTO(String symbol) {
        try {
            String data = getMarketData(symbol);
            if (data != null) {
                return objectMapper.readValue(data, MarketDataDTO.class);
            }
        } catch (Exception e) {
            log.error("Error parsing market data for symbol: {}", symbol, e);
        }
        return null;
    }

    public void saveMarketDataDTO(String symbol, MarketDataDTO dto) {
        try {
            String jsonData = objectMapper.writeValueAsString(dto);
            saveMarketData(symbol, jsonData);
        } catch (Exception e) {
            log.error("Error saving market data DTO for symbol: {}", symbol, e);
        }
    }

    /**
     * Get all stocks data from all indices, deduplicated by symbol.
     * Returns a JSON string with all unique stocks.
     */
    public String getAllStocks() {
        // All major NSE indices
        java.util.List<String> indices = java.util.List.of(
                "NIFTY 50", "NIFTY NEXT 50", "NIFTY 100", "NIFTY 200", "NIFTY 500",
                "NIFTY MIDCAP 50", "NIFTY MIDCAP 100", "NIFTY SMLCAP 100",
                "NIFTY BANK", "NIFTY IT", "NIFTY AUTO", "NIFTY FINANCIAL SERVICES",
                "NIFTY FMCG", "NIFTY PHARMA", "NIFTY METAL", "NIFTY MEDIA",
                "NIFTY ENERGY", "NIFTY PSU BANK", "NIFTY PRIVATE BANK",
                "NIFTY INFRA", "NIFTY REALTY", "NIFTY CONSUMPTION");

        java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> allStocks = new java.util.LinkedHashMap<>();

        log.info("getAllStocks: Starting to fetch stocks from {} indices", indices.size());

        for (String indexName : indices) {
            try {
                String indexData = getMarketData(indexName);
                if (indexData != null) {
                    log.debug("getAllStocks: Got data for index {}, length: {}", indexName, indexData.length());
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(indexData);
                    com.fasterxml.jackson.databind.JsonNode dataArray = root.path("data");

                    if (dataArray.isArray()) {
                        int countBefore = allStocks.size();
                        for (com.fasterxml.jackson.databind.JsonNode stock : dataArray) {
                            String symbol = stock.path("symbol").asText();
                            // Skip index entries and empty symbols
                            if (symbol != null && !symbol.isEmpty() && !symbol.startsWith("NIFTY")) {
                                allStocks.putIfAbsent(symbol, stock);
                            }
                        }
                        log.debug("getAllStocks: Index {} added {} stocks (total now: {})",
                                indexName, allStocks.size() - countBefore, allStocks.size());
                    }
                } else {
                    log.debug("getAllStocks: No cached data for index {}", indexName);
                }
            } catch (Exception e) {
                log.warn("Error processing index {}: {}", indexName, e.getMessage());
            }
        }

        log.info("getAllStocks: Collected {} unique stocks from all indices", allStocks.size());

        try {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("data", allStocks.values());
            result.put("totalCount", allStocks.size());
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error creating all stocks response", e);
            return null;
        }
    }
}
