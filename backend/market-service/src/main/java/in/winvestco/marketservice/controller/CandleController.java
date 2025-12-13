package in.winvestco.marketservice.controller;

import in.winvestco.marketservice.dto.CandleDTO;
import in.winvestco.marketservice.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for candle (OHLCV) data endpoints.
 * Provides historical and latest candle data for charting.
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Slf4j
public class CandleController {

    private final CandleService candleService;

    /**
     * Get historical candles for a symbol.
     * 
     * @param symbol   Stock symbol (e.g., RELIANCE)
     * @param interval Candle interval: 5m, 15m, 1h, 1d (default: 5m)
     * @param from     Start time (optional, defaults to 7 days ago)
     * @param to       End time (optional, defaults to now)
     * @param limit    Max number of candles (optional, used if from/to not
     *                 specified)
     * @return List of candles in Lightweight Charts format
     */
    @GetMapping("/candles")
    public ResponseEntity<List<CandleDTO>> getCandles(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "5m") String interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false, defaultValue = "500") Integer limit) {
        log.debug("Getting candles for {} interval {} from {} to {}", symbol, interval, from, to);

        // Validate interval
        if (!isValidInterval(interval)) {
            return ResponseEntity.badRequest().build();
        }

        List<CandleDTO> candles;

        if (from != null || to != null) {
            candles = candleService.getCandles(symbol, interval, from, to);
        } else {
            candles = candleService.getRecentCandles(symbol, interval, limit);
        }

        return ResponseEntity.ok(candles);
    }

    /**
     * Get the latest candle for a symbol.
     * Used for real-time chart updates.
     * 
     * @param symbol   Stock symbol
     * @param interval Candle interval (default: 5m)
     * @return Latest candle data
     */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestCandle(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "5m") String interval) {
        log.debug("Getting latest candle for {} interval {}", symbol, interval);

        return candleService.getLatestCandle(symbol, interval)
                .map(candle -> ResponseEntity.ok(Map.of(
                        "candle", candle,
                        "symbol", symbol,
                        "interval", interval,
                        "timestamp", System.currentTimeMillis())))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get available intervals.
     */
    @GetMapping("/intervals")
    public ResponseEntity<List<Map<String, String>>> getIntervals() {
        return ResponseEntity.ok(List.of(
                Map.of("value", "5m", "label", "5 Minutes"),
                Map.of("value", "15m", "label", "15 Minutes"),
                Map.of("value", "1h", "label", "1 Hour"),
                Map.of("value", "1d", "label", "1 Day")));
    }

    private boolean isValidInterval(String interval) {
        return interval != null && (interval.equals(CandleService.INTERVAL_5M) ||
                interval.equals(CandleService.INTERVAL_15M) ||
                interval.equals(CandleService.INTERVAL_1H) ||
                interval.equals(CandleService.INTERVAL_1D));
    }
}
