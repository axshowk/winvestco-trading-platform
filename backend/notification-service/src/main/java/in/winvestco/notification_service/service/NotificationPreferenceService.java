package in.winvestco.notification_service.service;

import in.winvestco.notification_service.dto.MuteSettingsDTO;
import in.winvestco.notification_service.model.NotificationPreference;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing notification preferences.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /**
     * Check if a notification type is muted for a user.
     */
    @Transactional(readOnly = true)
    public boolean isNotificationMuted(Long userId, NotificationType type) {
        // Security notifications cannot be muted
        if (!type.isMutable()) {
            return false;
        }

        return preferenceRepository.findByUserId(userId)
                .map(pref -> pref.isTypeMuted(type))
                .orElse(false);
    }

    /**
     * Get mute settings for a user.
     */
    @Transactional(readOnly = true)
    public MuteSettingsDTO getMuteSettings(Long userId) {
        log.debug("Getting mute settings for user: {}", userId);

        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder()
                        .userId(userId)
                        .muteAll(false)
                        .mutedTypes(new String[0])
                        .build());

        List<NotificationType> mutedTypes = pref.getMutedTypes() != null
                ? Arrays.stream(pref.getMutedTypes())
                        .map(NotificationType::valueOf)
                        .collect(Collectors.toList())
                : List.of();

        // Security notifications that cannot be muted
        List<NotificationType> unmutableTypes = Arrays.stream(NotificationType.values())
                .filter(t -> !t.isMutable())
                .collect(Collectors.toList());

        return MuteSettingsDTO.builder()
                .userId(userId)
                .muteAll(pref.getMuteAll())
                .mutedTypes(mutedTypes)
                .unmutableTypes(unmutableTypes)
                .build();
    }

    /**
     * Mute a specific notification type.
     */
    @Transactional
    public MuteSettingsDTO muteType(Long userId, NotificationType type) {
        log.info("Muting notification type {} for user {}", type, userId);

        if (!type.isMutable()) {
            log.warn("Cannot mute security notification type: {}", type);
            return getMuteSettings(userId);
        }

        NotificationPreference pref = getOrCreatePreference(userId);
        pref.muteType(type);
        preferenceRepository.save(pref);

        return getMuteSettings(userId);
    }

    /**
     * Unmute a specific notification type.
     */
    @Transactional
    public MuteSettingsDTO unmuteType(Long userId, NotificationType type) {
        log.info("Unmuting notification type {} for user {}", type, userId);

        NotificationPreference pref = getOrCreatePreference(userId);
        pref.unmuteType(type);
        preferenceRepository.save(pref);

        return getMuteSettings(userId);
    }

    /**
     * Mute all notifications.
     */
    @Transactional
    public MuteSettingsDTO muteAll(Long userId) {
        log.info("Muting all notifications for user {}", userId);

        NotificationPreference pref = getOrCreatePreference(userId);
        pref.muteAllNotifications();
        preferenceRepository.save(pref);

        return getMuteSettings(userId);
    }

    /**
     * Unmute all notifications.
     */
    @Transactional
    public MuteSettingsDTO unmuteAll(Long userId) {
        log.info("Unmuting all notifications for user {}", userId);

        NotificationPreference pref = getOrCreatePreference(userId);
        pref.unmuteAllNotifications();
        preferenceRepository.save(pref);

        return getMuteSettings(userId);
    }

    /**
     * Get or create preference for user.
     */
    private NotificationPreference getOrCreatePreference(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreference newPref = NotificationPreference.builder()
                            .userId(userId)
                            .muteAll(false)
                            .mutedTypes(new String[0])
                            .build();
                    return preferenceRepository.save(newPref);
                });
    }
}
