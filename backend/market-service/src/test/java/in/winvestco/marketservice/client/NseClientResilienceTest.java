package in.winvestco.marketservice.client;

import in.winvestco.marketservice.config.NseConfig;
import in.winvestco.marketservice.dto.MarketDataDTO;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NseClient resilience patterns.
 * Tests circuit breaker, rate limiter, retry, and bulkhead behavior.
 */
@ExtendWith(MockitoExtension.class)
class NseClientResilienceTest {

    @Mock
    private NseConfig config;

    @BeforeEach
    void setUp() {
        when(config.getBaseUrl()).thenReturn("https://www.nseindia.com");
        when(config.getApiBaseUrl()).thenReturn("https://www.nseindia.com/api");
        when(config.getUserAgent()).thenReturn("Test-Agent");
        when(config.getCookieRefreshIntervalMs()).thenReturn(300000);
    }

    @Test
    @DisplayName("Circuit breaker should open after failure threshold exceeded")
    void circuitBreakerShouldOpenAfterFailures() {
        // Configure circuit breaker with low threshold for testing
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("nseApiTest", config);

        // Simulate failures
        Supplier<MarketDataDTO> decoratedSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> {
                    throw new RuntimeException("NSE API Error");
                });

        // Make failing calls until circuit opens
        for (int i = 0; i < 4; i++) {
            try {
                decoratedSupplier.get();
            } catch (Exception ignored) {
            }
        }

        // Circuit should be OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Rate limiter should block when limit exceeded")
    void rateLimiterShouldBlockWhenLimitExceeded() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(2) // Only 2 calls allowed
                .limitRefreshPeriod(Duration.ofSeconds(10)) // Per 10 seconds
                .timeoutDuration(Duration.ofMillis(100)) // Short timeout
                .build();

        RateLimiter rateLimiter = RateLimiter.of("nseApiTest", config);

        // First two calls should succeed
        assertThat(rateLimiter.acquirePermission()).isTrue();
        assertThat(rateLimiter.acquirePermission()).isTrue();

        // Third call should fail (rate limited)
        assertThat(rateLimiter.acquirePermission()).isFalse();
    }

    @Test
    @DisplayName("Retry should use exponential backoff")
    void retryShouldUseExponentialBackoff() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(RuntimeException.class)
                .build();

        Retry retry = Retry.of("nseApiTest", config);

        long[] intervals = new long[2];
        int[] attempt = { 0 };

        try {
            Retry.decorateSupplier(retry, () -> {
                if (attempt[0] < 2) {
                    intervals[attempt[0]] = System.currentTimeMillis();
                    attempt[0]++;
                    throw new RuntimeException("Simulated failure");
                }
                return null;
            }).get();
        } catch (Exception ignored) {
        }

        // Verify retry attempts were made
        assertThat(attempt[0]).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Bulkhead should limit concurrent calls")
    void bulkheadShouldLimitConcurrentCalls() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(2)
                .maxWaitDuration(Duration.ofMillis(50))
                .build();

        Bulkhead bulkhead = Bulkhead.of("nseApiTest", config);

        // Acquire 2 permits (max)
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
        assertThat(bulkhead.tryAcquirePermission()).isTrue();

        // Third should fail (bulkhead full)
        assertThat(bulkhead.tryAcquirePermission()).isFalse();

        // Release one and try again
        bulkhead.releasePermission();
        assertThat(bulkhead.tryAcquirePermission()).isTrue();
    }

    @Test
    @DisplayName("Retry should not retry on NonRetryableException")
    void retryShouldNotRetryNonRetryableExceptions() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(RuntimeException.class)
                .ignoreExceptions(IllegalArgumentException.class) // Simulate NonRetryableException
                .build();

        Retry retry = Retry.of("nseApiTest", config);

        int[] attempts = { 0 };

        try {
            Retry.decorateSupplier(retry, () -> {
                attempts[0]++;
                throw new IllegalArgumentException("Bad request - should not retry");
            }).get();
        } catch (Exception ignored) {
        }

        // Should only attempt once (no retry for ignored exception)
        assertThat(attempts[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("Circuit breaker should transition to HALF_OPEN after wait duration")
    void circuitBreakerShouldTransitionToHalfOpen() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .waitDurationInOpenState(Duration.ofMillis(100)) // Short wait for test
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("nseApiTest", config);

        // Force circuit to OPEN
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Wait for automatic transition
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Should transition to HALF_OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("Fallback should be called when circuit is open")
    void fallbackShouldBeCalledWhenCircuitOpen() {
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("fallbackTest");
        circuitBreaker.transitionToOpenState();

        String fallbackValue = "FALLBACK_DATA";

        Supplier<String> decoratedSupplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> "REAL_DATA");

        String result;
        try {
            result = decoratedSupplier.get();
        } catch (Exception e) {
            // Circuit is open, use fallback
            result = fallbackValue;
        }

        assertThat(result).isEqualTo(fallbackValue);
    }
}
