package in.winvestco.notification_service.service;

import in.winvestco.notification_service.config.NotificationChannelConfig;
import in.winvestco.notification_service.dto.NotificationDeliveryDTO;
import in.winvestco.notification_service.model.*;
import in.winvestco.notification_service.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for tracking and managing notification deliveries.
 * Provides delivery guarantees with status tracking and audit trail.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationDeliveryTracker {

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationChannelConfig config;

    /**
     * Create delivery records for a notification across specified channels.
     */
    @Transactional
    public List<NotificationDelivery> createDeliveryRecords(Notification notification,
            Set<DeliveryChannel> channels,
            Map<DeliveryChannel, String> destinations) {
        List<NotificationDelivery> deliveries = new ArrayList<>();

        for (DeliveryChannel channel : channels) {
            NotificationDelivery delivery = NotificationDelivery.builder()
                    .notification(notification)
                    .channel(channel)
                    .destination(destinations.get(channel))
                    .status(DeliveryStatus.PENDING)
                    .maxAttempts(config.getDelivery().getMaxRetries())
                    .build();

            deliveries.add(deliveryRepository.save(delivery));
            log.debug("Created delivery record for notification {} channel {}",
                    notification.getId(), channel);
        }

        return deliveries;
    }

    /**
     * Record a successful delivery.
     */
    @Transactional
    public void recordSuccess(Long notificationId, DeliveryChannel channel) {
        deliveryRepository.findByNotificationIdAndChannel(notificationId, channel)
                .ifPresent(delivery -> {
                    delivery.markDelivered();
                    deliveryRepository.save(delivery);
                    log.info("Delivery successful: notification={} channel={}", notificationId, channel);
                });
    }

    /**
     * Record a failed delivery attempt.
     * 
     * @return true if retry is scheduled, false if max retries exceeded
     */
    @Transactional
    public boolean recordFailure(Long notificationId, DeliveryChannel channel,
            String errorMessage, String errorCode) {
        return deliveryRepository.findByNotificationIdAndChannel(notificationId, channel)
                .map(delivery -> {
                    long retryDelay = delivery.calculateRetryDelay(
                            config.getDelivery().getRetryDelayMs(),
                            config.getDelivery().getRetryBackoffMultiplier());
                    boolean willRetry = delivery.recordFailure(errorMessage, errorCode, retryDelay);
                    deliveryRepository.save(delivery);

                    if (willRetry) {
                        log.warn("Delivery failed, scheduling retry: notification={} channel={} " +
                                "attempt={} nextRetry={}",
                                notificationId, channel, delivery.getAttemptCount(),
                                delivery.getNextRetryAt());
                    } else {
                        log.error("Delivery permanently failed: notification={} channel={} " +
                                "attempts={} error={}",
                                notificationId, channel, delivery.getAttemptCount(), errorMessage);
                    }
                    return willRetry;
                })
                .orElse(false);
    }

    /**
     * Skip a delivery (e.g., channel disabled, user offline).
     */
    @Transactional
    public void skipDelivery(Long notificationId, DeliveryChannel channel, String reason) {
        deliveryRepository.findByNotificationIdAndChannel(notificationId, channel)
                .ifPresent(delivery -> {
                    delivery.skip(reason);
                    deliveryRepository.save(delivery);
                    log.debug("Delivery skipped: notification={} channel={} reason={}",
                            notificationId, channel, reason);
                });
    }

    /**
     * Mark delivery as in progress.
     */
    @Transactional
    public void markInProgress(Long deliveryId) {
        deliveryRepository.findById(deliveryId)
                .ifPresent(delivery -> {
                    delivery.markInProgress();
                    deliveryRepository.save(delivery);
                });
    }

    /**
     * Get delivery status for a notification.
     */
    @Transactional(readOnly = true)
    public List<NotificationDeliveryDTO> getDeliveryStatus(Long notificationId) {
        return deliveryRepository.findByNotificationId(notificationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get delivery status for a notification and specific channel.
     */
    @Transactional(readOnly = true)
    public Optional<NotificationDeliveryDTO> getDeliveryStatus(Long notificationId,
            DeliveryChannel channel) {
        return deliveryRepository.findByNotificationIdAndChannel(notificationId, channel)
                .map(this::toDTO);
    }

    /**
     * Get pending deliveries for a user (for delivery on reconnection).
     */
    @Transactional(readOnly = true)
    public List<NotificationDelivery> getPendingDeliveriesForUser(Long userId) {
        return deliveryRepository.findPendingByUserId(userId);
    }

    /**
     * Get delivery statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDeliveryStats(Instant since) {
        Map<String, Object> stats = new HashMap<>();

        // Count by status
        Map<DeliveryStatus, Long> statusCounts = new EnumMap<>(DeliveryStatus.class);
        deliveryRepository.countByStatusSince(since).forEach(row -> {
            statusCounts.put((DeliveryStatus) row[0], (Long) row[1]);
        });
        stats.put("byStatus", statusCounts);

        // Pending by channel
        Map<DeliveryChannel, Long> pendingByChannel = new EnumMap<>(DeliveryChannel.class);
        deliveryRepository.countPendingByChannel().forEach(row -> {
            pendingByChannel.put((DeliveryChannel) row[0], (Long) row[1]);
        });
        stats.put("pendingByChannel", pendingByChannel);

        return stats;
    }

    /**
     * Check if a notification was delivered via any channel.
     */
    @Transactional(readOnly = true)
    public boolean wasDelivered(Long notificationId) {
        return deliveryRepository.findByNotificationId(notificationId).stream()
                .anyMatch(d -> d.getStatus() == DeliveryStatus.DELIVERED);
    }

    /**
     * Move old failed deliveries to dead letter queue.
     */
    @Transactional
    public int archiveFailedDeliveries(Instant olderThan) {
        int moved = deliveryRepository.moveToDeadLetter(olderThan);
        if (moved > 0) {
            log.info("Moved {} failed deliveries to dead letter queue", moved);
        }
        return moved;
    }

    /**
     * Clean up old dead letter entries.
     */
    @Transactional
    public int cleanupDeadLetters(Instant olderThan) {
        int deleted = deliveryRepository.deleteOldDeadLetters(olderThan);
        if (deleted > 0) {
            log.info("Deleted {} old dead letter entries", deleted);
        }
        return deleted;
    }

    /**
     * Convert entity to DTO.
     */
    private NotificationDeliveryDTO toDTO(NotificationDelivery delivery) {
        return NotificationDeliveryDTO.builder()
                .id(delivery.getId())
                .notificationId(delivery.getNotification().getId())
                .channel(delivery.getChannel())
                .status(delivery.getStatus())
                .destination(maskDestination(delivery.getDestination(), delivery.getChannel()))
                .attemptCount(delivery.getAttemptCount())
                .maxAttempts(delivery.getMaxAttempts())
                .createdAt(delivery.getCreatedAt())
                .firstAttemptedAt(delivery.getFirstAttemptedAt())
                .lastAttemptedAt(delivery.getLastAttemptedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .nextRetryAt(delivery.getNextRetryAt())
                .errorMessage(delivery.getErrorMessage())
                .errorCode(delivery.getErrorCode())
                .build();
    }

    /**
     * Mask sensitive destination information.
     */
    private String maskDestination(String destination, DeliveryChannel channel) {
        if (destination == null) {
            return null;
        }
        return switch (channel) {
            case EMAIL -> maskEmail(destination);
            case SMS -> maskPhone(destination);
            case PUSH -> destination.length() > 20 ? destination.substring(0, 10) + "..." : destination;
            case WEBSOCKET -> "websocket";
        };
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    private String maskPhone(String phone) {
        if (phone.length() < 6) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
