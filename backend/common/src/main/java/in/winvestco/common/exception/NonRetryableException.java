package in.winvestco.common.exception;

/**
 * Marker exception for operations that should NOT be retried.
 * 
 * Used by Resilience4j retry configuration to exclude these exceptions
 * from retry attempts. Typically wraps:
 * - Client errors (4xx HTTP status)
 * - Validation failures
 * - Business rule violations
 * - Idempotency conflicts
 * 
 * @see io.github.resilience4j.retry.Retry
 */
public class NonRetryableException extends RuntimeException {

    public NonRetryableException(String message) {
        super(message);
    }

    public NonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create for validation errors
     */
    public static NonRetryableException validationError(String message) {
        return new NonRetryableException("Validation failed: " + message);
    }

    /**
     * Create for business rule violations
     */
    public static NonRetryableException businessRuleViolation(String message) {
        return new NonRetryableException("Business rule violation: " + message);
    }

    /**
     * Create for idempotency conflicts (duplicate request)
     */
    public static NonRetryableException idempotencyConflict(String idempotencyKey) {
        return new NonRetryableException("Duplicate request with idempotency key: " + idempotencyKey);
    }
}
