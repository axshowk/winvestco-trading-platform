package in.winvestco.notification_service.dto;

import in.winvestco.notification_service.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for notification mute settings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuteSettingsDTO {

    private Long userId;
    private Boolean muteAll;
    private List<NotificationType> mutedTypes;
    private List<NotificationType> unmutableTypes; // Types that cannot be muted (security)
}
