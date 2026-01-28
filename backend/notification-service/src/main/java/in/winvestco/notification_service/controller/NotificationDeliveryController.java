package in.winvestco.notification_service.controller;

import in.winvestco.notification_service.dto.NotificationDeliveryDTO;
import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.DeliveryStatus;
import in.winvestco.notification_service.repository.NotificationDeliveryRepository;
import in.winvestco.notification_service.service.NotificationDeliveryTracker;
import in.winvestco.notification_service.service.NotificationRetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * REST controller for notification delivery status and management.
 */
@RestController
@RequestMapping("/api/v1/notifications/delivery")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Delivery", description = "Delivery tracking and retry management")
public class NotificationDeliveryController {

    private final NotificationDeliveryTracker deliveryTracker;
    private final NotificationRetryService retryService;
    private final NotificationDeliveryRepository deliveryRepository;

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get delivery status for a notification")
    public ResponseEntity<List<NotificationDeliveryDTO>> getDeliveryStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long notificationId) {
        log.debug("Getting delivery status for notification: {}", notificationId);
        return ResponseEntity.ok(deliveryTracker.getDeliveryStatus(notificationId));
    }

    @GetMapping("/{notificationId}/{channel}")
    @Operation(summary = "Get delivery status for a specific channel")
    public ResponseEntity<NotificationDeliveryDTO> getChannelDeliveryStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long notificationId,
            @PathVariable DeliveryChannel channel) {
        return deliveryTracker.getDeliveryStatus(notificationId, channel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get delivery statistics")
    public ResponseEntity<Map<String, Object>> getDeliveryStats(
            @RequestParam(defaultValue = "24") int hoursBack) {
        Instant since = Instant.now().minus(hoursBack, ChronoUnit.HOURS);
        return ResponseEntity.ok(deliveryTracker.getDeliveryStats(since));
    }

    @GetMapping("/failed")
    @Operation(summary = "Get recent failed deliveries")
    public ResponseEntity<Page<NotificationDeliveryDTO>> getFailedDeliveries(
            Pageable pageable) {
        return ResponseEntity.ok(
                deliveryRepository.findByStatus(DeliveryStatus.FAILED, pageable)
                        .map(d -> NotificationDeliveryDTO.builder()
                                .id(d.getId())
                                .notificationId(d.getNotification().getId())
                                .channel(d.getChannel())
                                .status(d.getStatus())
                                .attemptCount(d.getAttemptCount())
                                .maxAttempts(d.getMaxAttempts())
                                .lastAttemptedAt(d.getLastAttemptedAt())
                                .errorMessage(d.getErrorMessage())
                                .errorCode(d.getErrorCode())
                                .build()));
    }

    @GetMapping("/dead-letter")
    @Operation(summary = "Get dead letter queue contents")
    public ResponseEntity<Page<NotificationDeliveryDTO>> getDeadLetterQueue(
            Pageable pageable) {
        return ResponseEntity.ok(
                deliveryRepository.findByStatus(DeliveryStatus.DEAD_LETTER, pageable)
                        .map(d -> NotificationDeliveryDTO.builder()
                                .id(d.getId())
                                .notificationId(d.getNotification().getId())
                                .channel(d.getChannel())
                                .status(d.getStatus())
                                .attemptCount(d.getAttemptCount())
                                .lastAttemptedAt(d.getLastAttemptedAt())
                                .errorMessage(d.getErrorMessage())
                                .build()));
    }

    @PostMapping("/{deliveryId}/retry")
    @Operation(summary = "Manually retry a failed delivery")
    public ResponseEntity<Map<String, Object>> manualRetry(
            @PathVariable Long deliveryId) {
        log.info("Manual retry requested for delivery: {}", deliveryId);
        boolean initiated = retryService.manualRetry(deliveryId);

        if (initiated) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Retry initiated for delivery " + deliveryId,
                    "deliveryId", deliveryId));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Delivery not found or not eligible for retry"));
        }
    }

    @PostMapping("/user/{userId}/deliver-pending")
    @Operation(summary = "Deliver pending notifications to a reconnected user")
    public ResponseEntity<Map<String, Object>> deliverPendingToUser(
            @PathVariable Long userId) {
        log.info("Delivering pending notifications to user: {}", userId);
        int delivered = retryService.deliverPendingToUser(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "delivered", delivered,
                "message", delivered + " pending notifications delivered"));
    }

    @GetMapping("/pending/count")
    @Operation(summary = "Get count of pending retries")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        return ResponseEntity.ok(Map.of(
                "pendingRetries", retryService.getPendingRetryCount()));
    }

    @GetMapping("/health")
    @Operation(summary = "Get delivery system health status")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Instant lastHour = Instant.now().minus(1, ChronoUnit.HOURS);
        Map<String, Object> stats = deliveryTracker.getDeliveryStats(lastHour);

        // Calculate health score
        @SuppressWarnings("unchecked")
        Map<DeliveryStatus, Long> byStatus = (Map<DeliveryStatus, Long>) stats.get("byStatus");

        long delivered = byStatus.getOrDefault(DeliveryStatus.DELIVERED, 0L);
        long failed = byStatus.getOrDefault(DeliveryStatus.FAILED, 0L);
        long total = delivered + failed;

        double successRate = total > 0 ? (double) delivered / total * 100 : 100;
        String healthStatus = successRate >= 95 ? "HEALTHY" : successRate >= 80 ? "DEGRADED" : "UNHEALTHY";

        return ResponseEntity.ok(Map.of(
                "status", healthStatus,
                "successRate", String.format("%.2f%%", successRate),
                "lastHour", stats,
                "pendingRetries", retryService.getPendingRetryCount()));
    }
}
