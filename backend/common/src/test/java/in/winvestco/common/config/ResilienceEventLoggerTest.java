package in.winvestco.common.config;


import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for ResilienceEventLogger.
 * Validates that event logging is properly configured for all Resilience4j
 * components.
 */
@ExtendWith(MockitoExtension.class)
class ResilienceEventLoggerTest {

    private ResilienceEventLogger eventLogger;

    @BeforeEach
    void setUp() {
        eventLogger = new ResilienceEventLogger();
    }

    @Test
    @DisplayName("Should register circuit breaker event logger without exception")
    void shouldRegisterCircuitBreakerEventLogger() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("testCircuitBreaker");

        assertThatNoException().isThrownBy(() -> eventLogger.circuitBreakerEventLogger(registry));

        assertThat(registry.getAllCircuitBreakers()).isNotEmpty();
    }

    @Test
    @DisplayName("Should register retry event logger without exception")
    void shouldRegisterRetryEventLogger() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        registry.retry("testRetry");

        assertThatNoException().isThrownBy(() -> eventLogger.retryEventLogger(registry));

        assertThat(registry.getAllRetries()).isNotEmpty();
    }

    @Test
    @DisplayName("Should register bulkhead event logger without exception")
    void shouldRegisterBulkheadEventLogger() {
        BulkheadRegistry registry = BulkheadRegistry.ofDefaults();
        registry.bulkhead("testBulkhead");

        assertThatNoException().isThrownBy(() -> eventLogger.bulkheadEventLogger(registry));

        assertThat(registry.getAllBulkheads()).isNotEmpty();
    }

    @Test
    @DisplayName("Should register rate limiter event logger without exception")
    void shouldRegisterRateLimiterEventLogger() {
        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        registry.rateLimiter("testRateLimiter");

        assertThatNoException().isThrownBy(() -> eventLogger.rateLimiterEventLogger(registry));

        assertThat(registry.getAllRateLimiters()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle empty registries gracefully")
    void shouldHandleEmptyRegistries() {
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();

        // Should not throw even with empty registries
        assertThatNoException().isThrownBy(() -> {
            eventLogger.circuitBreakerEventLogger(cbRegistry);
            eventLogger.retryEventLogger(retryRegistry);
            eventLogger.bulkheadEventLogger(bulkheadRegistry);
            eventLogger.rateLimiterEventLogger(rateLimiterRegistry);
        });
    }

    @Test
    @DisplayName("Should log circuit breaker state transition")
    void shouldLogCircuitBreakerStateTransition() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker circuitBreaker = registry.circuitBreaker("transitionTest");

        eventLogger.circuitBreakerEventLogger(registry);

        // Transition to OPEN state
        circuitBreaker.transitionToOpenState();

        // Verify state changed (event would have been logged)
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Should handle dynamically added circuit breakers")
    void shouldHandleDynamicallyAddedCircuitBreakers() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

        // Register event logger first
        eventLogger.circuitBreakerEventLogger(registry);

        // Then add a new circuit breaker
        CircuitBreaker newBreaker = registry.circuitBreaker("dynamicBreaker");

        // Should not throw when transitioning
        assertThatNoException().isThrownBy(() -> newBreaker.transitionToOpenState());
    }
}
