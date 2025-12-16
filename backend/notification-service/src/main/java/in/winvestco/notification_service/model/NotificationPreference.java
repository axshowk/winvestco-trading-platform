package in.winvestco.notification_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * User notification preferences entity.
 * Stores mute settings for notifications.
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "mute_all", nullable = false)
    @Builder.Default
    private Boolean muteAll = false;

    @Column(name = "muted_types", columnDefinition = "text[]")
    @Builder.Default
    private String[] mutedTypes = new String[0];

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Check if a specific notification type is muted.
     */
    public boolean isTypeMuted(NotificationType type) {
        if (!type.isMutable()) {
            return false; // Security notifications cannot be muted
        }
        if (muteAll) {
            return true;
        }
        if (mutedTypes == null) {
            return false;
        }
        String typeName = type.name();
        for (String mutedType : mutedTypes) {
            if (typeName.equals(mutedType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a notification type to muted list.
     */
    public void muteType(NotificationType type) {
        if (!type.isMutable()) {
            return; // Cannot mute security notifications
        }
        if (isTypeMuted(type)) {
            return; // Already muted
        }
        List<String> types = mutedTypes == null ? new ArrayList<>() : new ArrayList<>(List.of(mutedTypes));
        types.add(type.name());
        mutedTypes = types.toArray(new String[0]);
        updatedAt = Instant.now();
    }

    /**
     * Remove a notification type from muted list.
     */
    public void unmuteType(NotificationType type) {
        if (mutedTypes == null || mutedTypes.length == 0) {
            return;
        }
        List<String> types = new ArrayList<>(List.of(mutedTypes));
        types.remove(type.name());
        mutedTypes = types.toArray(new String[0]);
        updatedAt = Instant.now();
    }

    /**
     * Mute all notifications.
     */
    public void muteAllNotifications() {
        this.muteAll = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Unmute all notifications.
     */
    public void unmuteAllNotifications() {
        this.muteAll = false;
        this.mutedTypes = new String[0];
        this.updatedAt = Instant.now();
    }
}
