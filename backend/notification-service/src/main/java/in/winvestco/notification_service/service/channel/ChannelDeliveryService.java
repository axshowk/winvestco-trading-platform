package in.winvestco.notification_service.service.channel;

import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.model.DeliveryChannel;

/**
 * Interface for notification delivery channel implementations.
 */
public interface ChannelDeliveryService {

    /**
     * Get the delivery channel this service handles.
     */
    DeliveryChannel getChannel();

    /**
     * Check if this channel is currently enabled and configured.
     */
    boolean isEnabled();

    /**
     * Send notification to a specific user via this channel.
     *
     * @param userId       Target user ID
     * @param notification Notification to send
     * @param destination  Channel-specific destination (email, phone, fcmToken)
     * @return true if delivery was successful
     */
    boolean send(Long userId, NotificationDTO notification, String destination);

    /**
     * Validate destination format for this channel.
     */
    boolean isValidDestination(String destination);

    /**
     * Get display name for this channel.
     */
    default String getDisplayName() {
        return getChannel().name();
    }
}
