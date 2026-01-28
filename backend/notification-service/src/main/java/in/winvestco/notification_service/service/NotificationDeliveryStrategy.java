package in.winvestco.notification_service.service;

import in.winvestco.notification_service.config.NotificationChannelConfig;
import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.NotificationChannel;
import in.winvestco.notification_service.model.NotificationPriority;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.repository.NotificationChannelRepository;
import in.winvestco.notification_service.service.channel.ChannelDeliveryService;
import in.winvestco.notification_service.service.channel.EmailNotificationService;
import in.winvestco.notification_service.service.channel.PushNotificationService;
import in.winvestco.notification_service.service.channel.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Notification delivery strategy service.
 * Coordinates delivery across multiple channels based on user preferences
 * and notification priority.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDeliveryStrategy {

    private final NotificationChannelRepository channelRepository;
    private final NotificationChannelConfig config;
    private final WebSocketNotificationService webSocketService;
    private final PushNotificationService pushService;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final NotificationDeliveryTracker deliveryTracker;

    // Default priority mapping for notification types
    private static final Map<NotificationType, NotificationPriority> TYPE_PRIORITY_MAP = Map.ofEntries(
            // Critical - security and margin
            Map.entry(NotificationType.USER_LOGIN, NotificationPriority.CRITICAL),
            Map.entry(NotificationType.USER_PASSWORD_CHANGED, NotificationPriority.CRITICAL),
            Map.entry(NotificationType.USER_STATUS_CHANGED, NotificationPriority.CRITICAL),

            // High - order/trade execution
            Map.entry(NotificationType.ORDER_FILLED, NotificationPriority.HIGH),
            Map.entry(NotificationType.ORDER_PARTIALLY_FILLED, NotificationPriority.HIGH),
            Map.entry(NotificationType.TRADE_EXECUTED, NotificationPriority.HIGH),
            Map.entry(NotificationType.ORDER_REJECTED, NotificationPriority.HIGH),
            Map.entry(NotificationType.TRADE_FAILED, NotificationPriority.HIGH),

            // Medium - order lifecycle
            Map.entry(NotificationType.ORDER_CREATED, NotificationPriority.MEDIUM),
            Map.entry(NotificationType.ORDER_VALIDATED, NotificationPriority.MEDIUM),
            Map.entry(NotificationType.ORDER_CANCELLED, NotificationPriority.MEDIUM),
            Map.entry(NotificationType.ORDER_EXPIRED, NotificationPriority.MEDIUM),
            Map.entry(NotificationType.FUNDS_DEPOSITED, NotificationPriority.MEDIUM),
            Map.entry(NotificationType.FUNDS_WITHDRAWN, NotificationPriority.MEDIUM),

            // Low - informational
            Map.entry(NotificationType.FUNDS_LOCKED, NotificationPriority.LOW),
            Map.entry(NotificationType.FUNDS_RELEASED, NotificationPriority.LOW),
            Map.entry(NotificationType.PAYMENT_SUCCESS, NotificationPriority.MEDIUM),
            Map.entry(NotificationType.PAYMENT_FAILED, NotificationPriority.HIGH),
            Map.entry(NotificationType.PAYMENT_EXPIRED, NotificationPriority.MEDIUM));

    /**
     * Deliver notification to user via appropriate channels.
     * Uses async delivery when enabled.
     */
    public void deliver(Long userId, NotificationDTO notification) {
        NotificationPriority priority = getPriority(notification.getType());

        log.info("Delivering notification {} to user {} with priority {}",
                notification.getId(), userId, priority);

        if (config.getDelivery().isAsyncEnabled()) {
            deliverAsync(userId, notification, priority);
        } else {
            deliverSync(userId, notification, priority);
        }
    }

    /**
     * Synchronous delivery to all enabled channels.
     */
    private void deliverSync(Long userId, NotificationDTO notification, NotificationPriority priority) {
        Set<DeliveryChannel> channels = determineChannels(userId, notification.getType(), priority);

        for (DeliveryChannel channel : channels) {
            try {
                deliverToChannel(userId, notification, channel);
            } catch (Exception e) {
                log.error("Failed to deliver to channel {} for user {}: {}",
                        channel, userId, e.getMessage());
            }
        }
    }

    /**
     * Asynchronous delivery to all enabled channels.
     */
    @Async("notificationDeliveryExecutor")
    public void deliverAsync(Long userId, NotificationDTO notification, NotificationPriority priority) {
        Set<DeliveryChannel> channels = determineChannels(userId, notification.getType(), priority);

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (DeliveryChannel channel : channels) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return deliverToChannel(userId, notification, channel);
                } catch (Exception e) {
                    log.error("Async delivery failed for channel {} user {}: {}",
                            channel, userId, e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }

        // Wait for all deliveries to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    long successCount = futures.stream()
                            .map(f -> f.getNow(false))
                            .filter(Boolean::booleanValue)
                            .count();
                    log.info("Notification {} delivered to {}/{} channels for user {}",
                            notification.getId(), successCount, channels.size(), userId);
                });
    }

    /**
     * Deliver to a specific channel with delivery tracking.
     */
    private boolean deliverToChannel(Long userId, NotificationDTO notification, DeliveryChannel channel) {
        NotificationChannel channelConfig = getChannelConfig(userId, notification.getType());
        String destination = getDestination(channelConfig, channel);

        boolean success = false;
        String errorMessage = null;

        try {
            success = switch (channel) {
                case WEBSOCKET -> {
                    webSocketService.sendToUser(userId, notification);
                    yield true; // WebSocket is fire-and-forget
                }
                case PUSH -> {
                    if (destination != null) {
                        yield pushService.send(userId, notification, destination);
                    }
                    yield false;
                }
                case EMAIL -> {
                    if (destination != null) {
                        yield emailService.send(userId, notification, destination);
                    }
                    yield false;
                }
                case SMS -> {
                    if (destination != null) {
                        yield smsService.send(userId, notification, destination);
                    }
                    yield false;
                }
            };
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("Delivery failed for channel {} user {}: {}", channel, userId, errorMessage);
        }

        // Record delivery result for tracking
        if (deliveryTracker != null) {
            if (success) {
                deliveryTracker.recordSuccess(notification.getId(), channel);
            } else if (destination == null) {
                deliveryTracker.skipDelivery(notification.getId(), channel, "No destination configured");
            } else {
                deliveryTracker.recordFailure(notification.getId(), channel,
                        errorMessage != null ? errorMessage : "Delivery failed", "DELIVERY_FAILED");
            }
        }

        return success;
    }

    /**
     * Get destination for a channel from user config.
     */
    private String getDestination(NotificationChannel channelConfig, DeliveryChannel channel) {
        if (channelConfig == null) {
            return null;
        }
        return switch (channel) {
            case WEBSOCKET -> "websocket";
            case PUSH -> channelConfig.getFcmToken();
            case EMAIL -> channelConfig.getEmailAddress();
            case SMS -> channelConfig.getPhoneNumber();
        };
    }

    /**
     * Determine which channels to use for delivery based on:
     * 1. User preferences
     * 2. Notification priority
     * 3. Channel availability
     */
    private Set<DeliveryChannel> determineChannels(Long userId, NotificationType type,
            NotificationPriority priority) {
        Set<DeliveryChannel> channels = new HashSet<>();

        // WebSocket is always attempted for real-time delivery
        channels.add(DeliveryChannel.WEBSOCKET);

        // Get user's channel preferences
        Optional<NotificationChannel> userChannelOpt = channelRepository
                .findByUserIdAndNotificationType(userId, type);

        if (userChannelOpt.isPresent()) {
            // Use user preferences
            NotificationChannel userChannel = userChannelOpt.get();
            channels.addAll(userChannel.getEnabledChannels());
        } else {
            // Fall back to priority-based defaults
            if (priority.isPushImmediate() && pushService.isEnabled()) {
                channels.add(DeliveryChannel.PUSH);
            }
            if (priority.isEmailEnabled() && emailService.isEnabled()) {
                channels.add(DeliveryChannel.EMAIL);
            }
            if (priority.isSmsEnabled() && smsService.isEnabled()) {
                channels.add(DeliveryChannel.SMS);
            }
        }

        // Critical notifications always use all available channels
        if (priority == NotificationPriority.CRITICAL) {
            if (pushService.isEnabled())
                channels.add(DeliveryChannel.PUSH);
            if (emailService.isEnabled())
                channels.add(DeliveryChannel.EMAIL);
            if (smsService.isEnabled() && smsService.isSmsEligible(type)) {
                channels.add(DeliveryChannel.SMS);
            }
        }

        log.debug("Determined channels for user {} type {} priority {}: {}",
                userId, type, priority, channels);
        return channels;
    }

    /**
     * Get the priority for a notification type.
     */
    public NotificationPriority getPriority(NotificationType type) {
        return TYPE_PRIORITY_MAP.getOrDefault(type, NotificationPriority.MEDIUM);
    }

    /**
     * Get channel configuration for user and notification type.
     */
    private NotificationChannel getChannelConfig(Long userId, NotificationType type) {
        return channelRepository.findByUserIdAndNotificationType(userId, type).orElse(null);
    }

    /**
     * Get all available delivery services.
     */
    public List<ChannelDeliveryService> getAvailableChannels() {
        List<ChannelDeliveryService> available = new ArrayList<>();
        if (pushService.isEnabled())
            available.add(pushService);
        if (emailService.isEnabled())
            available.add(emailService);
        if (smsService.isEnabled())
            available.add(smsService);
        return available;
    }

    /**
     * Check which channels are currently enabled globally.
     */
    public Map<DeliveryChannel, Boolean> getChannelStatus() {
        Map<DeliveryChannel, Boolean> status = new EnumMap<>(DeliveryChannel.class);
        status.put(DeliveryChannel.WEBSOCKET, true); // Always available
        status.put(DeliveryChannel.PUSH, pushService.isEnabled());
        status.put(DeliveryChannel.EMAIL, emailService.isEnabled());
        status.put(DeliveryChannel.SMS, smsService.isEnabled());
        return status;
    }
}
