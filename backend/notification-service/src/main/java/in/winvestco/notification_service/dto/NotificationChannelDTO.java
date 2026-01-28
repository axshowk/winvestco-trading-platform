package in.winvestco.notification_service.dto;

import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for notification channel preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationChannelDTO {

    private Long id;
    private Long userId;
    private NotificationType notificationType;
    private Boolean websocketEnabled;
    private Boolean pushEnabled;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private String emailAddress;
    private String phoneNumber;
    private boolean hasFcmToken;
    private Set<DeliveryChannel> enabledChannels;
}
