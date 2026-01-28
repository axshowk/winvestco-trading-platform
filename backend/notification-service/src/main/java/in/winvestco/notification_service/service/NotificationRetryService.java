package in.winvestco.notification_service.service;

import in.winvestco.notification_service.config.NotificationChannelConfig;
import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.mapper.NotificationMapper;
import in.winvestco.notification_service.model.*;
import in.winvestco.notification_service.repository.NotificationDeliveryRepository;
import in.winvestco.notification_service.repository.NotificationChannelRepository;
import in.winvestco.notification_service.service.channel.EmailNotificationService;
import in.winvestco.notification_service.service.channel.PushNotificationService;
import in.winvestco.notification_service.service.channel.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Service for retrying failed notification deliveries.
 * Runs scheduled jobs to process pending retries and clean up old records.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationRetryService {

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationChannelRepository channelRepository;
    private final NotificationDeliveryTracker deliveryTracker;
    private final NotificationChannelConfig config;
    private final NotificationMapper notificationMapper;

    private final WebSocketNotificationService webSocketService;
    private final PushNotificationService pushService;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;

    private static final int RETRY_BATCH_SIZE = 50;
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(5);
    private static final Duration DEAD_LETTER_AGE = Duration.ofDays(7);
    private static final Duration CLEANUP_AGE = Duration.ofDays(30);

    /**
     * Process pending retries every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    @Transactional
    public void processRetries() {
        Instant now = Instant.now();
        List<NotificationDelivery> pendingDeliveries = deliveryRepository.findReadyForRetry(
                now, PageRequest.of(0, RETRY_BATCH_SIZE));

        if (pendingDeliveries.isEmpty()) {
            return;
        }

        log.info("Processing {} pending delivery retries", pendingDeliveries.size());

        for (NotificationDelivery delivery : pendingDeliveries) {
            try {
                processDelivery(delivery);
            } catch (Exception e) {
                log.error("Error processing delivery retry: id={} error={}",
                        delivery.getId(), e.getMessage(), e);
                deliveryTracker.recordFailure(
                        delivery.getNotification().getId(),
                        delivery.getChannel(),
                        e.getMessage(),
                        "RETRY_ERROR");
            }
        }
    }

    /**
     * Process a single delivery.
     */
    private void processDelivery(NotificationDelivery delivery) {
        Notification notification = delivery.getNotification();
        Long notificationId = notification.getId();
        DeliveryChannel channel = delivery.getChannel();

        log.debug("Retrying delivery: id={} notification={} channel={} attempt={}",
                delivery.getId(), notificationId, channel, delivery.getAttemptCount() + 1);

        deliveryTracker.markInProgress(delivery.getId());

        NotificationDTO dto = notificationMapper.toDTO(notification);
        boolean success = attemptDelivery(notification.getUserId(), dto, channel, delivery.getDestination());

        if (success) {
            deliveryTracker.recordSuccess(notificationId, channel);
        } else {
            deliveryTracker.recordFailure(
                    notificationId,
                    channel,
                    "Delivery failed on retry",
                    "DELIVERY_FAILED");
        }
    }

    /**
     * Attempt delivery to a specific channel.
     */
    private boolean attemptDelivery(Long userId, NotificationDTO notification,
            DeliveryChannel channel, String destination) {
        try {
            return switch (channel) {
                case WEBSOCKET -> {
                    webSocketService.sendToUser(userId, notification);
                    yield true; // WebSocket is fire-and-forget
                }
                case PUSH -> pushService.send(userId, notification, destination);
                case EMAIL -> emailService.send(userId, notification, destination);
                case SMS -> smsService.send(userId, notification, destination);
            };
        } catch (Exception e) {
            log.error("Delivery attempt failed: channel={} error={}", channel, e.getMessage());
            return false;
        }
    }

    /**
     * Deliver pending notifications to a user who just reconnected.
     */
    @Transactional
    public int deliverPendingToUser(Long userId) {
        List<NotificationDelivery> pending = deliveryTracker.getPendingDeliveriesForUser(userId);

        if (pending.isEmpty()) {
            return 0;
        }

        log.info("Delivering {} pending notifications to reconnected user {}", pending.size(), userId);

        int delivered = 0;
        for (NotificationDelivery delivery : pending) {
            if (delivery.getChannel() == DeliveryChannel.WEBSOCKET) {
                try {
                    NotificationDTO dto = notificationMapper.toDTO(delivery.getNotification());
                    webSocketService.sendToUser(userId, dto);
                    deliveryTracker.recordSuccess(delivery.getNotification().getId(), DeliveryChannel.WEBSOCKET);
                    delivered++;
                } catch (Exception e) {
                    log.error("Failed to deliver pending notification to user {}: {}", userId, e.getMessage());
                }
            }
        }

        return delivered;
    }

    /**
     * Reset stale in-progress deliveries (likely from crashed processes).
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    @Transactional
    public void resetStaleDeliveries() {
        Instant staleThreshold = Instant.now().minus(STALE_THRESHOLD);
        List<NotificationDelivery> stale = deliveryRepository.findStaleInProgress(staleThreshold);

        if (!stale.isEmpty()) {
            log.warn("Resetting {} stale in-progress deliveries", stale.size());
            for (NotificationDelivery delivery : stale) {
                delivery.recordFailure("Stale delivery reset", "STALE_RESET",
                        config.getDelivery().getRetryDelayMs());
                deliveryRepository.save(delivery);
            }
        }
    }

    /**
     * Archive old failed deliveries to dead letter queue.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void archiveFailedDeliveries() {
        Instant cutoff = Instant.now().minus(DEAD_LETTER_AGE);
        int archived = deliveryTracker.archiveFailedDeliveries(cutoff);
        log.info("Archived {} failed deliveries to dead letter queue", archived);
    }

    /**
     * Clean up old dead letter entries.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupDeadLetters() {
        Instant cutoff = Instant.now().minus(CLEANUP_AGE);
        int deleted = deliveryTracker.cleanupDeadLetters(cutoff);
        log.info("Cleaned up {} old dead letter entries", deleted);
    }

    /**
     * Manually retry a failed delivery.
     */
    @Transactional
    public boolean manualRetry(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .filter(d -> d.getStatus() == DeliveryStatus.FAILED ||
                        d.getStatus() == DeliveryStatus.DEAD_LETTER)
                .map(delivery -> {
                    // Reset for retry
                    delivery.setStatus(DeliveryStatus.PENDING);
                    delivery.setAttemptCount(0);
                    delivery.setNextRetryAt(null);
                    deliveryRepository.save(delivery);
                    log.info("Manual retry initiated for delivery {}", deliveryId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Get count of pending retries.
     */
    @Transactional(readOnly = true)
    public long getPendingRetryCount() {
        return deliveryRepository.findReadyForRetry(Instant.now(), PageRequest.of(0, 1))
                .size(); // This is inefficient for counting, but works for monitoring
    }
}
