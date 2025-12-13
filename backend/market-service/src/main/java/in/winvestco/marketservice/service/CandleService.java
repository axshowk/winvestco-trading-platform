package in.winvestco.marketservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.marketservice.dto.CandleDTO;
import in.winvestco.marketservice.entity.Candle;
import in.winvestco.marketservice.repository.CandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing candle (OHLCV) data.
 * Handles storage, retrieval, and aggregation of candle data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandleService {

    private final CandleRepository candleRepository;
    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Supported intervals
    public static final String INTERVAL_5M = "5m";
    public static final String INTERVAL_15M = "15m";
    public static final String INTERVAL_1H = "1h";
    public static final String INTERVAL_1D = "1d";

    /**
     * Store candles for all stocks from the current market data.
     * Called by the scheduler after fetching NSE data.
     */
    @Transactional
    public void storeCurrentCandles() {
        try {
            String allStocksData = marketDataService.getAllStocks();
            if (allStocksData == null) {
                log.warn("No stocks data available for candle storage");
                return;
            }

            JsonNode root = objectMapper.readTree(allStocksData);
            JsonNode dataArray = root.path("data");

            if (!dataArray.isArray()) {
                log.warn("Invalid data format for candle storage");
                return;
            }

            // Round timestamp to 5-minute interval
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime roundedTime = now.truncatedTo(ChronoUnit.HOURS)
                    .plusMinutes((now.getMinute() / 5) * 5);

            int savedCount = 0;
            for (JsonNode stock : dataArray) {
                try {
                    String symbol = stock.path("symbol").asText();
                    if (symbol == null || symbol.isEmpty())
                        continue;

                    BigDecimal lastPrice = getBigDecimal(stock, "lastPrice");
                    BigDecimal open = getBigDecimal(stock, "open");
                    BigDecimal high = getBigDecimal(stock, "dayHigh");
                    BigDecimal low = getBigDecimal(stock, "dayLow");
                    Long volume = stock.path("totalTradedVolume").asLong(0L);

                    // Skip if we already have a candle for this time
                    if (candleRepository.existsBySymbolAndIntervalTypeAndTimestamp(
                            symbol, INTERVAL_5M, roundedTime)) {
                        continue;
                    }

                    Candle candle = Candle.builder()
                            .symbol(symbol)
                            .intervalType(INTERVAL_5M)
                            .timestamp(roundedTime)
                            .open(open != null && open.compareTo(BigDecimal.ZERO) > 0 ? open : lastPrice)
                            .high(high != null && high.compareTo(BigDecimal.ZERO) > 0 ? high : lastPrice)
                            .low(low != null && low.compareTo(BigDecimal.ZERO) > 0 ? low : lastPrice)
                            .close(lastPrice)
                            .volume(volume)
                            .build();

                    candleRepository.save(candle);
                    savedCount++;
                } catch (Exception e) {
                    log.debug("Error saving candle for stock: {}", e.getMessage());
                }
            }

            log.info("Stored {} candles for 5m interval at {}", savedCount, roundedTime);

        } catch (Exception e) {
            log.error("Error storing candles: {}", e.getMessage(), e);
        }
    }

    /**
     * Get candles for a symbol and interval within a time range.
     */
    public List<CandleDTO> getCandles(String symbol, String interval, LocalDateTime from, LocalDateTime to) {
        // Default to last 7 days if not specified
        if (from == null) {
            from = LocalDateTime.now().minusDays(7);
        }
        if (to == null) {
            to = LocalDateTime.now();
        }

        List<Candle> candles = candleRepository
                .findBySymbolAndIntervalTypeAndTimestampBetweenOrderByTimestampAsc(
                        symbol, interval, from, to);

        return candles.stream()
                .map(CandleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get the latest candle for a symbol.
     */
    public Optional<CandleDTO> getLatestCandle(String symbol, String interval) {
        return candleRepository
                .findFirstBySymbolAndIntervalTypeOrderByTimestampDesc(symbol, interval)
                .map(CandleDTO::fromEntity);
    }

    /**
     * Get recent candles (limited count) for a symbol.
     */
    public List<CandleDTO> getRecentCandles(String symbol, String interval, int limit) {
        List<Candle> candles = candleRepository.findRecentCandles(symbol, interval, limit);
        // Reverse to get ascending order for charting
        return candles.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .map(CandleDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Aggregate 5-minute candles into larger intervals.
     * Should be called periodically (e.g., every hour).
     */
    @Transactional
    public void aggregateCandles() {
        // This can be implemented later for 15m, 1h, 1d aggregation
        // For now, we focus on 5m candles as the base interval
        log.debug("Candle aggregation placeholder - implement as needed");
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
}
