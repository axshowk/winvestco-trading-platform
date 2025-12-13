package in.winvestco.notification_service.dto;

import in.winvestco.notification_service.model.NotificationStatus;
import in.winvestco.notification_service.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for Notification entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private Map<String, Object> data;
    private NotificationStatus status;
    private Instant createdAt;
    private Instant readAt;
}
