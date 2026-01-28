package in.winvestco.notification_service.service;

import in.winvestco.notification_service.dto.NotificationChannelDTO;
import in.winvestco.notification_service.dto.UpdateChannelPreferencesRequest;
import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.NotificationChannel;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.repository.NotificationChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing notification channel preferences.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationChannelService {

    private final NotificationChannelRepository channelRepository;

    /**
     * Get all channel preferences for a user.
     */
    @Transactional(readOnly = true)
    public List<NotificationChannelDTO> getUserChannelPreferences(Long userId) {
        log.debug("Getting channel preferences for user: {}", userId);
        return channelRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get channel preference for a specific notification type.
     */
    @Transactional(readOnly = true)
    public Optional<NotificationChannelDTO> getChannelPreference(Long userId, NotificationType type) {
        return channelRepository.findByUserIdAndNotificationType(userId, type)
                .map(this::toDTO);
    }

    /**
     * Update channel preferences for a notification type.
     */
    @Transactional
    public NotificationChannelDTO updateChannelPreference(Long userId, UpdateChannelPreferencesRequest request) {
        log.info("Updating channel preferences for user {}, type {}", userId, request.getNotificationType());

        NotificationChannel channel = channelRepository
                .findByUserIdAndNotificationType(userId, request.getNotificationType())
                .orElseGet(() -> NotificationChannel.builder()
                        .userId(userId)
                        .notificationType(request.getNotificationType())
                        .build());

        // Update enabled channels
        if (request.getEnabledChannels() != null) {
            Set<DeliveryChannel> enabledChannels = request.getEnabledChannels();
            channel.setWebsocketEnabled(enabledChannels.contains(DeliveryChannel.WEBSOCKET));
            channel.setPushEnabled(enabledChannels.contains(DeliveryChannel.PUSH));
            channel.setEmailEnabled(enabledChannels.contains(DeliveryChannel.EMAIL));
            channel.setSmsEnabled(enabledChannels.contains(DeliveryChannel.SMS));
        }

        // Update contact info
        if (request.getEmailAddress() != null) {
            channel.setEmailAddress(request.getEmailAddress());
        }
        if (request.getPhoneNumber() != null) {
            channel.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getFcmToken() != null) {
            channel.setFcmToken(request.getFcmToken());
        }

        NotificationChannel saved = channelRepository.save(channel);
        log.info("Updated channel preferences for user {}: {}", userId, saved.getEnabledChannels());
        return toDTO(saved);
    }

    /**
     * Update FCM token for all notification types.
     */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        log.info("Updating FCM token for user {}", userId);
        List<NotificationChannel> channels = channelRepository.findByUserId(userId);

        for (NotificationChannel channel : channels) {
            channel.setFcmToken(fcmToken);
        }

        channelRepository.saveAll(channels);
    }

    /**
     * Update email address for all notification types.
     */
    @Transactional
    public void updateEmailAddress(Long userId, String emailAddress) {
        log.info("Updating email address for user {}", userId);
        List<NotificationChannel> channels = channelRepository.findByUserId(userId);

        for (NotificationChannel channel : channels) {
            channel.setEmailAddress(emailAddress);
        }

        channelRepository.saveAll(channels);
    }

    /**
     * Update phone number for all notification types.
     */
    @Transactional
    public void updatePhoneNumber(Long userId, String phoneNumber) {
        log.info("Updating phone number for user {}", userId);
        List<NotificationChannel> channels = channelRepository.findByUserId(userId);

        for (NotificationChannel channel : channels) {
            channel.setPhoneNumber(phoneNumber);
        }

        channelRepository.saveAll(channels);
    }

    /**
     * Enable a channel for all notification types.
     */
    @Transactional
    public void enableChannelForAllTypes(Long userId, DeliveryChannel channel) {
        log.info("Enabling {} for all notification types for user {}", channel, userId);
        List<NotificationChannel> channels = channelRepository.findByUserId(userId);

        for (NotificationChannel nc : channels) {
            nc.enableChannel(channel);
        }

        channelRepository.saveAll(channels);
    }

    /**
     * Disable a channel for all notification types.
     */
    @Transactional
    public void disableChannelForAllTypes(Long userId, DeliveryChannel channel) {
        log.info("Disabling {} for all notification types for user {}", channel, userId);
        List<NotificationChannel> channels = channelRepository.findByUserId(userId);

        for (NotificationChannel nc : channels) {
            nc.disableChannel(channel);
        }

        channelRepository.saveAll(channels);
    }

    /**
     * Create default channel preferences for a new user.
     */
    @Transactional
    public void createDefaultPreferences(Long userId, String email, String phone) {
        log.info("Creating default channel preferences for user {}", userId);

        // Create preferences for critical notification types with all channels enabled
        for (NotificationType type : NotificationType.values()) {
            NotificationChannel channel = NotificationChannel.builder()
                    .userId(userId)
                    .notificationType(type)
                    .websocketEnabled(true)
                    .pushEnabled(false) // Push requires FCM token
                    .emailEnabled(email != null && !email.isBlank())
                    .smsEnabled(false) // SMS only for critical, opt-in
                    .emailAddress(email)
                    .phoneNumber(phone)
                    .build();

            channelRepository.save(channel);
        }
    }

    /**
     * Delete all channel preferences for a user.
     */
    @Transactional
    public void deleteUserPreferences(Long userId) {
        log.info("Deleting all channel preferences for user {}", userId);
        channelRepository.deleteByUserId(userId);
    }

    /**
     * Convert entity to DTO.
     */
    private NotificationChannelDTO toDTO(NotificationChannel channel) {
        return NotificationChannelDTO.builder()
                .id(channel.getId())
                .userId(channel.getUserId())
                .notificationType(channel.getNotificationType())
                .websocketEnabled(channel.getWebsocketEnabled())
                .pushEnabled(channel.getPushEnabled())
                .emailEnabled(channel.getEmailEnabled())
                .smsEnabled(channel.getSmsEnabled())
                .emailAddress(channel.getEmailAddress())
                .phoneNumber(maskPhoneNumber(channel.getPhoneNumber()))
                .hasFcmToken(channel.getFcmToken() != null && !channel.getFcmToken().isBlank())
                .enabledChannels(channel.getEnabledChannels())
                .build();
    }

    /**
     * Mask phone number for privacy.
     */
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 6) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
