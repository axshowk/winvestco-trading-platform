package in.winvestco.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j event logging configuration.
 * 
 * Logs state transitions and important events for observability:
 * - Circuit breaker state changes (CLOSED → OPEN → HALF_OPEN)
 * - Retry attempts with backoff duration
 * - Bulkhead rejections
 * - Rate limiter denials
 */
@Configuration
@ConditionalOnClass(CircuitBreakerRegistry.class)
@Slf4j
public class ResilienceEventLogger {

        @Autowired(required = false)
        private CircuitBreakerRegistry circuitBreakerRegistry;

        @Autowired(required = false)
        private RetryRegistry retryRegistry;

        @Autowired(required = false)
        private BulkheadRegistry bulkheadRegistry;

        @Autowired(required = false)
        private RateLimiterRegistry rateLimiterRegistry;

        @PostConstruct
        public void registerEventLoggers() {
                if (circuitBreakerRegistry != null) {
                        registerCircuitBreakerEventLoggers();
                }
                if (retryRegistry != null) {
                        registerRetryEventLoggers();
                }
                if (bulkheadRegistry != null) {
                        registerBulkheadEventLoggers();
                }
                if (rateLimiterRegistry != null) {
                        registerRateLimiterEventLoggers();
                }
                log.info("Resilience4j event loggers registered successfully");
        }

        public void circuitBreakerEventLogger(CircuitBreakerRegistry registry) {
                registry.getAllCircuitBreakers().forEach(this::registerCircuitBreakerEvents);
                registry.getEventPublisher()
                                .onEntryAdded(event -> registerCircuitBreakerEvents(event.getAddedEntry()));
        }

        public void retryEventLogger(RetryRegistry registry) {
                registry.getAllRetries().forEach(retry -> {
                        retry.getEventPublisher()
                                        .onRetry(event -> log.info("[RETRY] {} - Attempt {} after {}ms, exception: {}",
                                                        event.getName(),
                                                        event.getNumberOfRetryAttempts(),
                                                        event.getWaitInterval().toMillis(),
                                                        event.getLastThrowable() != null
                                                                        ? event.getLastThrowable().getMessage()
                                                                        : "N/A"))
                                        .onSuccess(event -> log.debug("[RETRY] {} - Success after {} attempts",
                                                        event.getName(),
                                                        event.getNumberOfRetryAttempts()))
                                        .onError(event -> log.warn("[RETRY] {} - Failed after {} attempts: {}",
                                                        event.getName(),
                                                        event.getNumberOfRetryAttempts(),
                                                        event.getLastThrowable().getMessage()))
                                        .onIgnoredError(event -> log.debug(
                                                        "[RETRY] {} - Ignored non-retryable error: {}",
                                                        event.getName(),
                                                        event.getLastThrowable().getClass().getSimpleName()));
                });
                registry.getEventPublisher().onEntryAdded(event -> {
                        var retry = event.getAddedEntry();
                        retry.getEventPublisher()
                                        .onRetry(e -> log.info("[RETRY] {} - Attempt {} after {}ms",
                                                        e.getName(), e.getNumberOfRetryAttempts(),
                                                        e.getWaitInterval().toMillis()));
                });
        }

        public void bulkheadEventLogger(BulkheadRegistry registry) {
                registry.getAllBulkheads().forEach(bulkhead -> {
                        bulkhead.getEventPublisher()
                                        .onCallRejected(event -> log.warn(
                                                        "[BULKHEAD] {} - Call rejected, available permits: {}",
                                                        event.getBulkheadName(),
                                                        bulkhead.getMetrics().getAvailableConcurrentCalls()))
                                        .onCallFinished(event -> log.trace("[BULKHEAD] {} - Call finished",
                                                        event.getBulkheadName()));
                });
        }

        public void rateLimiterEventLogger(RateLimiterRegistry registry) {
                registry.getAllRateLimiters().forEach(rateLimiter -> {
                        rateLimiter.getEventPublisher()
                                        .onSuccess(event -> log.trace("[RATE_LIMITER] {} - Request permitted",
                                                        event.getRateLimiterName()))
                                        .onFailure(event -> log.warn(
                                                        "[RATE_LIMITER] {} - Request denied, available permits: {}",
                                                        event.getRateLimiterName(),
                                                        rateLimiter.getMetrics().getAvailablePermissions()));
                });
        }

        private void registerCircuitBreakerEventLoggers() {
                circuitBreakerEventLogger(circuitBreakerRegistry);
        }

        private void registerRetryEventLoggers() {
                retryEventLogger(retryRegistry);
        }

        private void registerBulkheadEventLoggers() {
                bulkheadEventLogger(bulkheadRegistry);
        }

        private void registerRateLimiterEventLoggers() {
                rateLimiterEventLogger(rateLimiterRegistry);
        }

        private void registerCircuitBreakerEvents(CircuitBreaker circuitBreaker) {
                circuitBreaker.getEventPublisher()
                                .onStateTransition(event -> log.warn("[CIRCUIT_BREAKER] {} - State transition: {} → {}",
                                                event.getCircuitBreakerName(),
                                                event.getStateTransition().getFromState(),
                                                event.getStateTransition().getToState()))
                                .onSlowCallRateExceeded(
                                                event -> log.warn("[CIRCUIT_BREAKER] {} - Slow call rate exceeded: {}%",
                                                                event.getCircuitBreakerName(),
                                                                event.getSlowCallRate()))
                                .onFailureRateExceeded(
                                                event -> log.warn("[CIRCUIT_BREAKER] {} - Failure rate exceeded: {}%",
                                                                event.getCircuitBreakerName(),
                                                                event.getFailureRate()))
                                .onCallNotPermitted(event -> log.debug(
                                                "[CIRCUIT_BREAKER] {} - Call not permitted (circuit OPEN)",
                                                event.getCircuitBreakerName()));
        }
}
