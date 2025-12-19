package in.winvestco.marketservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import in.winvestco.marketservice.config.NseConfig;
import in.winvestco.marketservice.dto.MarketDataDTO;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Client for fetching market data from NSE India's free public API.
 * This API requires browser-like headers and session cookies for access.
 * 
 * Protected with Resilience4j patterns:
 * - Circuit Breaker: Fails fast when NSE API is unhealthy
 * - Rate Limiter: Prevents overwhelming NSE API (10 calls/second)
 * - Retry: Exponential backoff with jitter for transient failures
 * - Bulkhead: Limits concurrent calls to NSE API
 */
@Service
@Slf4j
public class NseClient {

    private final NseConfig config;
    private final RestTemplate restTemplate;
    private String sessionCookie;
    private long lastCookieRefresh = 0;

    public NseClient(NseConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetches index quote data for a given index name.
     * 
     * @param indexName The name of the index (e.g., "NIFTY 50", "NIFTY BANK")
     * @return MarketDataDTO with the index data, or null if fetch fails
     */
    @CircuitBreaker(name = "nseApi", fallbackMethod = "getIndexQuoteFallback")
    @RateLimiter(name = "nseApi")
    @Retry(name = "nseApi")
    @Bulkhead(name = "nseApi")
    public MarketDataDTO getIndexQuote(String indexName) {
        refreshCookieIfNeeded();

        String encodedIndex = URLEncoder.encode(indexName, StandardCharsets.UTF_8);
        String url = config.getApiBaseUrl() + "/equity-stockIndices?index=" + encodedIndex;

        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        log.debug("Fetching NSE data for: {} from URL: {}", indexName, url);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, JsonNode.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseIndexData(response.getBody(), indexName);
        }

        log.warn("Unsuccessful response for {}: {}", indexName, response.getStatusCode());
        return null;
    }

    /**
     * Fallback method when NSE API is unavailable or circuit is open
     */
    private MarketDataDTO getIndexQuoteFallback(String indexName, Throwable t) {
        log.warn("[FALLBACK] NSE API unavailable for {}: {}", indexName, t.getMessage());
        return null;
    }

    /**
     * Fetches quotes for multiple indices.
     */
    public List<MarketDataDTO> getMultipleQuotes(List<String> indexNames) {
        List<MarketDataDTO> results = new ArrayList<>();

        for (String indexName : indexNames) {
            MarketDataDTO data = getIndexQuote(indexName);
            if (data != null) {
                results.add(data);
            }
        }

        return results;
    }

    /**
     * Fetches the full NSE response for an index including all constituent stocks.
     * This returns the raw JSON string which can be cached and served to frontend.
     * 
     * @param indexName The name of the index (e.g., "NIFTY 50", "NIFTY BANK")
     * @return JSON string with full index data, or null if fetch fails
     */
    @CircuitBreaker(name = "nseApi", fallbackMethod = "getFullIndexDataFallback")
    @RateLimiter(name = "nseApi")
    @Retry(name = "nseApi")
    @Bulkhead(name = "nseApi")
    public String getFullIndexData(String indexName) {
        refreshCookieIfNeeded();

        String encodedIndex = URLEncoder.encode(indexName, StandardCharsets.UTF_8);
        String url = config.getApiBaseUrl() + "/equity-stockIndices?index=" + encodedIndex;

        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        log.debug("Fetching full NSE data for: {} from URL: {}", indexName, url);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.info("Successfully fetched full index data for: {}", indexName);
            return response.getBody();
        }

        log.warn("Unsuccessful response for full data {}: {}", indexName, response.getStatusCode());
        return null;
    }

    /**
     * Fallback method when NSE API is unavailable for full index data
     */
    private String getFullIndexDataFallback(String indexName, Throwable t) {
        log.warn("[FALLBACK] NSE API unavailable for full index data {}: {}", indexName, t.getMessage());
        return null;
    }

    /**
     * Refreshes the session cookie if it has expired or doesn't exist.
     * NSE requires a valid session cookie obtained from their main page.
     */
    private void refreshCookieIfNeeded() {
        long now = System.currentTimeMillis();
        if (sessionCookie == null || (now - lastCookieRefresh) > config.getCookieRefreshIntervalMs()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.USER_AGENT, config.getUserAgent());
                headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        config.getBaseUrl(), HttpMethod.GET, entity, String.class);

                List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                if (cookies != null && !cookies.isEmpty()) {
                    sessionCookie = cookies.stream()
                            .map(c -> c.split(";")[0])
                            .collect(Collectors.joining("; "));
                    lastCookieRefresh = now;
                    log.info("Refreshed NSE session cookie");
                }
            } catch (Exception e) {
                log.warn("Failed to refresh NSE session cookie: {}", e.getMessage());
            }
        }
    }

    /**
     * Creates HTTP headers for NSE API requests.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, config.getUserAgent());
        headers.set(HttpHeaders.ACCEPT, "application/json");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
        // Removed Accept-Encoding: gzip to ensure uncompressed JSON for Redis caching
        headers.set("Referer", config.getBaseUrl() + "/");
        if (sessionCookie != null) {
            headers.set(HttpHeaders.COOKIE, sessionCookie);
        }
        return headers;
    }

    /**
     * Parses the NSE API response JSON into a MarketDataDTO.
     */
    private MarketDataDTO parseIndexData(JsonNode root, String indexName) {
        try {
            JsonNode dataArray = root.path("data");
            if (dataArray.isArray() && dataArray.size() > 0) {
                // The first element is typically the index summary
                JsonNode indexData = dataArray.get(0);

                return MarketDataDTO.builder()
                        .symbol(indexName)
                        .exchange("NSE")
                        .tradingSymbol(indexName.replace(" ", "-"))
                        .lastTradedPrice(getBigDecimal(indexData, "lastPrice"))
                        .open(getBigDecimal(indexData, "open"))
                        .high(getBigDecimal(indexData, "dayHigh"))
                        .low(getBigDecimal(indexData, "dayLow"))
                        .close(getBigDecimal(indexData, "previousClose"))
                        .changeValue(getBigDecimal(indexData, "change"))
                        .changePercentage(getBigDecimal(indexData, "pChange"))
                        .volume(getLong(indexData, "totalTradedVolume"))
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            log.warn("No data array found in NSE response for: {}", indexName);
            return null;

        } catch (Exception e) {
            log.error("Error parsing NSE data for {}: {}", indexName, e.getMessage());
            return null;
        }
    }

    private BigDecimal getBigDecimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isMissingNode() && !value.isNull()) {
            try {
                return BigDecimal.valueOf(value.asDouble());
            } catch (Exception e) {
                log.debug("Could not parse {} as BigDecimal", field);
            }
        }
        return BigDecimal.ZERO;
    }

    private Long getLong(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isMissingNode() && !value.isNull()) {
            try {
                return value.asLong();
            } catch (Exception e) {
                log.debug("Could not parse {} as Long", field);
            }
        }
        return 0L;
    }
}
