package in.winvestco.notification_service.service.channel;

import in.winvestco.notification_service.config.NotificationChannelConfig;
import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.model.DeliveryChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Push notification delivery service using Firebase Cloud Messaging.
 * 
 * Note: This is a skeleton implementation. To fully enable FCM:
 * 1. Add firebase-admin dependency to pom.xml
 * 2. Configure Firebase credentials in application.yml
 * 3. Initialize FirebaseApp in a @PostConstruct method
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PushNotificationService implements ChannelDeliveryService {

    private final NotificationChannelConfig config;

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.PUSH;
    }

    @Override
    public boolean isEnabled() {
        return config.getPush().isEnabled()
                && config.getPush().getFirebaseProjectId() != null;
    }

    @Override
    public boolean send(Long userId, NotificationDTO notification, String fcmToken) {
        if (!isEnabled()) {
            log.debug("Push notifications are disabled, skipping delivery");
            return false;
        }

        if (!isValidDestination(fcmToken)) {
            log.warn("Invalid FCM token for user {}", userId);
            return false;
        }

        try {
            log.info("Sending push notification to user {} via FCM token: {}...",
                    userId, fcmToken.substring(0, Math.min(20, fcmToken.length())));

            // Build FCM message
            // In production, use Firebase Admin SDK:
            // Message message = Message.builder()
            // .setToken(fcmToken)
            // .setNotification(Notification.builder()
            // .setTitle(notification.getTitle())
            // .setBody(notification.getMessage())
            // .build())
            // .putData("notificationId", String.valueOf(notification.getId()))
            // .putData("type", notification.getType().name())
            // .setAndroidConfig(AndroidConfig.builder()
            // .setNotification(AndroidNotification.builder()
            // .setIcon(config.getPush().getDefaultIcon())
            // .setColor(config.getPush().getDefaultColor())
            // .build())
            // .build())
            // .build();
            // String response = FirebaseMessaging.getInstance().send(message);

            // Simulated success for now
            log.info("Push notification sent successfully to user {}, notification: {}",
                    userId, notification.getId());
            return true;

        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isValidDestination(String fcmToken) {
        // FCM tokens are typically 150+ characters
        return fcmToken != null && fcmToken.length() >= 100;
    }

    @Override
    public String getDisplayName() {
        return "Push Notification";
    }

    /**
     * Register a new FCM token for a device.
     * Called when user logs in on a new device.
     */
    public void registerToken(Long userId, String fcmToken, String deviceInfo) {
        log.info("Registering FCM token for user {}: device={}", userId, deviceInfo);
        // In production, store this in NotificationChannel entity
    }

    /**
     * Unregister an FCM token (e.g., on logout).
     */
    public void unregisterToken(Long userId, String fcmToken) {
        log.info("Unregistering FCM token for user {}", userId);
        // In production, remove from NotificationChannel entity
    }
}
