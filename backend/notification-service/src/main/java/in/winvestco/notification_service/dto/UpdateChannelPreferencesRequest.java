package in.winvestco.notification_service.dto;

import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request DTO for updating notification channel preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateChannelPreferencesRequest {

    private NotificationType notificationType;

    private Set<DeliveryChannel> enabledChannels;

    @Email(message = "Invalid email address")
    private String emailAddress;

    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    private String fcmToken;
}
