package in.winvestco.notification_service.controller;

import in.winvestco.notification_service.dto.NotificationChannelDTO;
import in.winvestco.notification_service.dto.UpdateChannelPreferencesRequest;
import in.winvestco.notification_service.model.DeliveryChannel;
import in.winvestco.notification_service.model.NotificationType;
import in.winvestco.notification_service.service.NotificationChannelService;
import in.winvestco.notification_service.service.NotificationDeliveryStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing notification channel preferences.
 */
@RestController
@RequestMapping("/api/v1/notifications/channels")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Channels", description = "Multi-channel notification preference management")
public class NotificationChannelController {

    private final NotificationChannelService channelService;
    private final NotificationDeliveryStrategy deliveryStrategy;

    @GetMapping
    @Operation(summary = "Get all channel preferences for user")
    public ResponseEntity<List<NotificationChannelDTO>> getChannelPreferences(
            @RequestHeader("X-User-Id") Long userId) {
        log.debug("Getting channel preferences for user: {}", userId);
        return ResponseEntity.ok(channelService.getUserChannelPreferences(userId));
    }

    @GetMapping("/{type}")
    @Operation(summary = "Get channel preference for specific notification type")
    public ResponseEntity<NotificationChannelDTO> getChannelPreference(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable NotificationType type) {
        return channelService.getChannelPreference(userId, type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{type}")
    @Operation(summary = "Update channel preferences for a notification type")
    public ResponseEntity<NotificationChannelDTO> updateChannelPreference(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable NotificationType type,
            @Valid @RequestBody UpdateChannelPreferencesRequest request) {
        request.setNotificationType(type);
        log.info("Updating channel preferences for user {} type {}", userId, type);
        return ResponseEntity.ok(channelService.updateChannelPreference(userId, request));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Update channel preferences for multiple notification types")
    public ResponseEntity<List<NotificationChannelDTO>> updateBulkPreferences(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody List<UpdateChannelPreferencesRequest> requests) {
        log.info("Bulk updating channel preferences for user {}: {} types", userId, requests.size());

        List<NotificationChannelDTO> results = requests.stream()
                .map(request -> channelService.updateChannelPreference(userId, request))
                .toList();

        return ResponseEntity.ok(results);
    }

    @PatchMapping("/fcm-token")
    @Operation(summary = "Register or update FCM token for push notifications")
    public ResponseEntity<Map<String, String>> updateFcmToken(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String fcmToken = body.get("fcmToken");
        if (fcmToken == null || fcmToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "fcmToken is required"));
        }

        log.info("Updating FCM token for user {}", userId);
        channelService.updateFcmToken(userId, fcmToken);
        return ResponseEntity.ok(Map.of("status", "success", "message", "FCM token updated"));
    }

    @PatchMapping("/email")
    @Operation(summary = "Update email address for email notifications")
    public ResponseEntity<Map<String, String>> updateEmailAddress(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String email = body.get("emailAddress");

        log.info("Updating email address for user {}", userId);
        channelService.updateEmailAddress(userId, email);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Email address updated"));
    }

    @PatchMapping("/phone")
    @Operation(summary = "Update phone number for SMS notifications")
    public ResponseEntity<Map<String, String>> updatePhoneNumber(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {
        String phone = body.get("phoneNumber");

        log.info("Updating phone number for user {}", userId);
        channelService.updatePhoneNumber(userId, phone);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Phone number updated"));
    }

    @PostMapping("/enable/{channel}")
    @Operation(summary = "Enable a channel for all notification types")
    public ResponseEntity<Map<String, String>> enableChannel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable DeliveryChannel channel) {
        log.info("Enabling {} for user {}", channel, userId);
        channelService.enableChannelForAllTypes(userId, channel);
        return ResponseEntity.ok(Map.of("status", "success",
                "message", channel.name() + " enabled for all notification types"));
    }

    @PostMapping("/disable/{channel}")
    @Operation(summary = "Disable a channel for all notification types")
    public ResponseEntity<Map<String, String>> disableChannel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable DeliveryChannel channel) {
        log.info("Disabling {} for user {}", channel, userId);
        channelService.disableChannelForAllTypes(userId, channel);
        return ResponseEntity.ok(Map.of("status", "success",
                "message", channel.name() + " disabled for all notification types"));
    }

    @GetMapping("/status")
    @Operation(summary = "Get global channel availability status")
    public ResponseEntity<Map<DeliveryChannel, Boolean>> getChannelStatus() {
        return ResponseEntity.ok(deliveryStrategy.getChannelStatus());
    }

    @DeleteMapping
    @Operation(summary = "Delete all channel preferences (reset to defaults)")
    public ResponseEntity<Map<String, String>> deletePreferences(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Deleting all channel preferences for user {}", userId);
        channelService.deleteUserPreferences(userId);
        return ResponseEntity.ok(Map.of("status", "success",
                "message", "All channel preferences deleted"));
    }
}
