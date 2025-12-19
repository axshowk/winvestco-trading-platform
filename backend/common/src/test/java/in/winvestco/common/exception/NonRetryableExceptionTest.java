package in.winvestco.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for NonRetryableException.
 * Validates that the marker exception is properly structured for Resilience4j
 * retry exclusion.
 */
class NonRetryableExceptionTest {

    @Test
    @DisplayName("Should create NonRetryableException with message")
    void shouldCreateWithMessage() {
        String message = "Test error message";
        NonRetryableException exception = new NonRetryableException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should create NonRetryableException with message and cause")
    void shouldCreateWithMessageAndCause() {
        String message = "Test error message";
        Throwable cause = new IllegalArgumentException("Root cause");
        NonRetryableException exception = new NonRetryableException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should create validation error")
    void shouldCreateValidationError() {
        NonRetryableException exception = NonRetryableException.validationError("Invalid amount");

        assertThat(exception.getMessage()).contains("Validation failed");
        assertThat(exception.getMessage()).contains("Invalid amount");
    }

    @Test
    @DisplayName("Should create business rule violation")
    void shouldCreateBusinessRuleViolation() {
        NonRetryableException exception = NonRetryableException.businessRuleViolation("Insufficient funds");

        assertThat(exception.getMessage()).contains("Business rule violation");
        assertThat(exception.getMessage()).contains("Insufficient funds");
    }

    @Test
    @DisplayName("Should create idempotency conflict")
    void shouldCreateIdempotencyConflict() {
        String idempotencyKey = "wallet123:DEPOSIT:txn456";
        NonRetryableException exception = NonRetryableException.idempotencyConflict(idempotencyKey);

        assertThat(exception.getMessage()).contains("Duplicate request");
        assertThat(exception.getMessage()).contains(idempotencyKey);
    }

    @Test
    @DisplayName("NonRetryableException should be a RuntimeException for unchecked propagation")
    void shouldBeRuntimeException() {
        NonRetryableException exception = new NonRetryableException("test");

        // This ensures Resilience4j can catch it without requiring checked exception
        // handling
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).isNotInstanceOf(Exception.class).isInstanceOf(RuntimeException.class);
    }
}
