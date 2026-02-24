package in.winvestco.order_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.kafka.market.QuoteUpdatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketQuoteProjectionService {

    private static final String KEY_PREFIX = "market:quote:";
    private static final Duration QUOTE_TTL = Duration.ofMinutes(30);
    private static final Duration STALE_PROJECTION_THRESHOLD = Duration.ofMinutes(2);
    private static final String PROJECTION_NAME = "market_quote";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public void upsertQuote(QuoteUpdatedEvent event) {
        String symbol = normalizeSymbol(event.getSymbol());
        if (symbol.isEmpty()) {
            throw new IllegalArgumentException("Quote event symbol is missing");
        }

        QuoteSnapshot snapshot = new QuoteSnapshot(
                symbol,
                event.getExchange(),
                event.getLastPrice(),
                event.getOpen(),
                event.getHigh(),
                event.getLow(),
                event.getClose(),
                event.getChange(),
                event.getChangePercent(),
                event.getVolume(),
                event.getEventTime().getSeconds(),
                Instant.now().toEpochMilli());

        try {
            String payload = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(KEY_PREFIX + symbol, payload, QUOTE_TTL);
            recordProjectionUpdateDelay(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist quote projection for symbol " + symbol, e);
        }
    }

    public Optional<QuoteSnapshot> getQuote(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized.isEmpty()) {
            recordProjectionAccess("miss");
            return Optional.empty();
        }
        String payload = redisTemplate.opsForValue().get(KEY_PREFIX + normalized);
        if (payload == null || payload.isBlank()) {
            recordProjectionAccess("miss");
            return Optional.empty();
        }
        try {
            QuoteSnapshot snapshot = objectMapper.readValue(payload, QuoteSnapshot.class);
            recordProjectionAccess(isSnapshotStale(snapshot) ? "stale" : "hit");
            return Optional.of(snapshot);
        } catch (Exception e) {
            log.warn("Failed to read quote projection for symbol {}", normalized, e);
            recordProjectionAccess("miss");
            return Optional.empty();
        }
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        return symbol.trim().toUpperCase();
    }

    private void recordProjectionUpdateDelay(QuoteUpdatedEvent event) {
        if (!event.hasEventTime()) {
            return;
        }
        long eventTimeMs = event.getEventTime().getSeconds() * 1000L + (event.getEventTime().getNanos() / 1_000_000L);
        long delayMs = Math.max(0L, Instant.now().toEpochMilli() - eventTimeMs);
        Timer.builder("projection_update_delay_seconds")
                .description("Delay between event time and projection update")
                .tag("projection", PROJECTION_NAME)
                .register(meterRegistry)
                .record(Duration.ofMillis(delayMs));
    }

    private boolean isSnapshotStale(QuoteSnapshot snapshot) {
        long ageMs = Instant.now().toEpochMilli() - snapshot.projectionUpdatedAtMs();
        return ageMs > STALE_PROJECTION_THRESHOLD.toMillis();
    }

    private void recordProjectionAccess(String result) {
        meterRegistry.counter("projection_access_total", "projection", PROJECTION_NAME, "result", result).increment();
    }

    public record QuoteSnapshot(
            String symbol,
            String exchange,
            double lastPrice,
            double open,
            double high,
            double low,
            double close,
            double change,
            double changePercent,
            long volume,
            long eventTimeSeconds,
            long projectionUpdatedAtMs) {
    }
}
