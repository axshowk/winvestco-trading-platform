package in.winvestco.notification_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity tracking delivery status for each notification-channel combination.
 * Enables delivery guarantees with retry mechanism and audit trail.
 */
@Entity
@Table(name = "notification_deliveries", uniqueConstraints = @UniqueConstraint(columnNames = { "notification_id",
        "channel" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private DeliveryChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "destination")
    private String destination; // email, phone, fcm token, etc.

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "first_attempted_at")
    private Instant firstAttemptedAt;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_code")
    private String errorCode;

    /**
     * Record a successful delivery.
     */
    public void markDelivered() {
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = Instant.now();
        this.lastAttemptedAt = Instant.now();
        this.errorMessage = null;
        this.errorCode = null;
    }

    /**
     * Record a failed delivery attempt.
     * 
     * @param errorMessage Error description
     * @param errorCode    Optional error code
     * @param retryDelayMs Delay before next retry in milliseconds
     * @return true if retry is scheduled, false if max retries exceeded
     */
    public boolean recordFailure(String errorMessage, String errorCode, long retryDelayMs) {
        this.attemptCount++;
        this.lastAttemptedAt = Instant.now();
        if (this.firstAttemptedAt == null) {
            this.firstAttemptedAt = this.lastAttemptedAt;
        }
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;

        if (this.attemptCount >= this.maxAttempts) {
            this.status = DeliveryStatus.FAILED;
            return false;
        } else {
            this.status = DeliveryStatus.RETRYING;
            this.nextRetryAt = Instant.now().plusMillis(retryDelayMs);
            return true;
        }
    }

    /**
     * Move to dead letter queue.
     */
    public void moveToDeadLetter(String reason) {
        this.status = DeliveryStatus.DEAD_LETTER;
        this.errorMessage = reason;
    }

    /**
     * Skip this delivery.
     */
    public void skip(String reason) {
        this.status = DeliveryStatus.SKIPPED;
        this.errorMessage = reason;
    }

    /**
     * Mark as in progress (being processed).
     */
    public void markInProgress() {
        this.status = DeliveryStatus.IN_PROGRESS;
        this.lastAttemptedAt = Instant.now();
        if (this.firstAttemptedAt == null) {
            this.firstAttemptedAt = this.lastAttemptedAt;
        }
    }

    /**
     * Calculate retry delay with exponential backoff.
     * 
     * @param baseDelayMs Base delay in milliseconds
     * @param multiplier  Backoff multiplier
     * @return Delay in milliseconds
     */
    public long calculateRetryDelay(long baseDelayMs, double multiplier) {
        return (long) (baseDelayMs * Math.pow(multiplier, attemptCount));
    }

    /**
     * Check if delivery is ready for retry.
     */
    public boolean isReadyForRetry() {
        if (!status.isRetryable()) {
            return false;
        }
        if (nextRetryAt == null) {
            return true;
        }
        return Instant.now().isAfter(nextRetryAt);
    }

    /**
     * Get duration since first attempt.
     */
    public long getDurationSinceFirstAttemptMs() {
        if (firstAttemptedAt == null) {
            return 0;
        }
        return Instant.now().toEpochMilli() - firstAttemptedAt.toEpochMilli();
    }
}
