package in.winvestco.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Message payload for Redis Pub/Sub notification dispatch.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDispatchMessage implements Serializable {
    private Long userId;
    private NotificationDTO notification;
    // broadcast flag if we want to support broadcast
    private boolean broadcast;
}
