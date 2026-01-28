package in.winvestco.notification_service.model;

/**
 * Status of a notification delivery attempt.
 */
public enum DeliveryStatus {
    /**
     * Delivery is queued and waiting to be processed.
     */
    PENDING,

    /**
     * Delivery is currently in progress.
     */
    IN_PROGRESS,

    /**
     * Delivery was successful.
     */
    DELIVERED,

    /**
     * Delivery failed but will be retried.
     */
    RETRYING,

    /**
     * Delivery permanently failed (max retries exceeded).
     */
    FAILED,

    /**
     * Delivery was skipped (user offline, channel disabled, etc.).
     */
    SKIPPED,

    /**
     * Moved to dead letter queue after permanent failure.
     */
    DEAD_LETTER;

    /**
     * Check if this status indicates delivery is complete (success or permanent
     * failure).
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED || this == SKIPPED || this == DEAD_LETTER;
    }

    /**
     * Check if this status indicates delivery can be retried.
     */
    public boolean isRetryable() {
        return this == PENDING || this == RETRYING;
    }
}
