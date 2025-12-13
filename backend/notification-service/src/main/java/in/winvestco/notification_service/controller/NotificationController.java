package in.winvestco.notification_service.controller;

import in.winvestco.notification_service.dto.NotificationDTO;
import in.winvestco.notification_service.model.NotificationStatus;
import in.winvestco.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for notification management.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notifications for user")
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Getting notifications for user: {}", userId);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getNotificationsForUser(userId, pageable));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications for user")
    public ResponseEntity<Page<NotificationDTO>> getUnreadNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Getting unread notifications for user: {}", userId);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getNotificationsByStatus(
                userId, NotificationStatus.UNREAD, pageable));
    }

    @GetMapping("/count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User-Id") Long userId) {
        
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        
        log.info("Marking notification {} as read for user {}", id, userId);
        return notificationService.markAsRead(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("Marking all notifications as read for user {}", userId);
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("markedAsRead", count));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive a notification")
    public ResponseEntity<NotificationDTO> archiveNotification(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        
        log.info("Archiving notification {} for user {}", id, userId);
        return notificationService.archiveNotification(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> deleteNotification(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        
        log.info("Deleting notification {} for user {}", id, userId);
        boolean deleted = notificationService.deleteNotification(id, userId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
