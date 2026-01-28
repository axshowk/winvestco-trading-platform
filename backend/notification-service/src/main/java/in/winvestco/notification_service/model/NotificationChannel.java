package in.winvestco.notification_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity storing user's notification channel preferences per notification type.
 * Allows users to configure which channels (WebSocket, Push, Email, SMS)
 * they want to receive for each type of notification.
 */
@Entity
@Table(name = "notification_channels", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
        "notification_type" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "websocket_enabled", nullable = false)
    @Builder.Default
    private Boolean websocketEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = false;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = false;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private Boolean smsEnabled = false;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * Get the set of enabled delivery channels for this configuration.
     */
    public Set<DeliveryChannel> getEnabledChannels() {
        Set<DeliveryChannel> channels = new HashSet<>();
        if (Boolean.TRUE.equals(websocketEnabled)) {
            channels.add(DeliveryChannel.WEBSOCKET);
        }
        if (Boolean.TRUE.equals(pushEnabled) && fcmToken != null && !fcmToken.isBlank()) {
            channels.add(DeliveryChannel.PUSH);
        }
        if (Boolean.TRUE.equals(emailEnabled) && emailAddress != null && !emailAddress.isBlank()) {
            channels.add(DeliveryChannel.EMAIL);
        }
        if (Boolean.TRUE.equals(smsEnabled) && phoneNumber != null && !phoneNumber.isBlank()) {
            channels.add(DeliveryChannel.SMS);
        }
        return channels;
    }

    /**
     * Check if a specific channel is enabled.
     */
    public boolean isChannelEnabled(DeliveryChannel channel) {
        return switch (channel) {
            case WEBSOCKET -> Boolean.TRUE.equals(websocketEnabled);
            case PUSH -> Boolean.TRUE.equals(pushEnabled) && fcmToken != null;
            case EMAIL -> Boolean.TRUE.equals(emailEnabled) && emailAddress != null;
            case SMS -> Boolean.TRUE.equals(smsEnabled) && phoneNumber != null;
        };
    }

    /**
     * Enable a delivery channel.
     */
    public void enableChannel(DeliveryChannel channel) {
        switch (channel) {
            case WEBSOCKET -> websocketEnabled = true;
            case PUSH -> pushEnabled = true;
            case EMAIL -> emailEnabled = true;
            case SMS -> smsEnabled = true;
        }
        updatedAt = Instant.now();
    }

    /**
     * Disable a delivery channel.
     */
    public void disableChannel(DeliveryChannel channel) {
        switch (channel) {
            case WEBSOCKET -> websocketEnabled = false;
            case PUSH -> pushEnabled = false;
            case EMAIL -> emailEnabled = false;
            case SMS -> smsEnabled = false;
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
