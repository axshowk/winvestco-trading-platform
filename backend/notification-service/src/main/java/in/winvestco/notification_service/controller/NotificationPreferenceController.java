package in.winvestco.notification_service.controller;

import in.winvestco.notification_service.dto.MuteSettingsDTO;
import in.winvestco.notification_service.dto.MuteTypeRequest;
import in.winvestco.notification_service.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for notification preferences and mute settings.
 */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Preferences", description = "Notification mute settings APIs")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get mute settings for user")
    public ResponseEntity<MuteSettingsDTO> getMuteSettings(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.debug("Getting mute settings for user: {}", userId);
        return ResponseEntity.ok(preferenceService.getMuteSettings(userId));
    }

    @PostMapping("/mute-type")
    @Operation(summary = "Mute or unmute a specific notification type")
    public ResponseEntity<MuteSettingsDTO> updateMuteType(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MuteTypeRequest request) {
        
        log.info("Updating mute setting for user {}: type={}, mute={}", 
                userId, request.getType(), request.getMute());
        
        MuteSettingsDTO settings;
        if (request.getMute()) {
            settings = preferenceService.muteType(userId, request.getType());
        } else {
            settings = preferenceService.unmuteType(userId, request.getType());
        }
        
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/mute-all")
    @Operation(summary = "Mute all notifications")
    public ResponseEntity<MuteSettingsDTO> muteAll(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("Muting all notifications for user {}", userId);
        return ResponseEntity.ok(preferenceService.muteAll(userId));
    }

    @PostMapping("/unmute-all")
    @Operation(summary = "Unmute all notifications")
    public ResponseEntity<MuteSettingsDTO> unmuteAll(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("Unmuting all notifications for user {}", userId);
        return ResponseEntity.ok(preferenceService.unmuteAll(userId));
    }
}
