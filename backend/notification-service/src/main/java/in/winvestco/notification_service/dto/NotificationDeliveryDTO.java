package in.winvestco.notification_service.dto;

import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for notification delivery status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryDTO {

    private Long id;
    private Long notificationId;
    private DeliveryChannel channel;
    private DeliveryStatus status;
    private String destination;
    private Integer attemptCount;
    private Integer maxAttempts;
    private Instant createdAt;
    private Instant firstAttemptedAt;
    private Instant lastAttemptedAt;
    private Instant deliveredAt;
    private Instant nextRetryAt;
    private String errorMessage;
    private String errorCode;

    /**
     * Check if delivery was successful.
     */
    public boolean isSuccessful() {
        return status == DeliveryStatus.DELIVERED;
    }

    /**
     * Check if delivery is still pending.
     */
    public boolean isPending() {
        return status == DeliveryStatus.PENDING || status == DeliveryStatus.RETRYING;
    }

    /**
     * Get human-readable status description.
     */
    public String getStatusDescription() {
        return switch (status) {
            case PENDING -> "Waiting to be delivered";
            case IN_PROGRESS -> "Delivery in progress";
            case DELIVERED -> "Successfully delivered";
            case RETRYING -> String.format("Retrying (attempt %d of %d)", attemptCount + 1, maxAttempts);
            case FAILED -> String.format("Failed after %d attempts: %s", attemptCount, errorMessage);
            case SKIPPED -> "Skipped: " + errorMessage;
            case DEAD_LETTER -> "Moved to dead letter queue";
        };
    }
}
