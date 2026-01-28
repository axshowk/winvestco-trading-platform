package in.winvestco.notification_service.model;

/**
 * Delivery channels for notifications.
 * Notifications can be sent through multiple channels based on user
 * preferences.
 */
public enum DeliveryChannel {
    /**
     * Real-time WebSocket delivery (requires active browser session).
     */
    WEBSOCKET,

    /**
     * Push notification via Firebase Cloud Messaging (mobile/PWA).
     */
    PUSH,

    /**
     * Email notification (async delivery).
     */
    EMAIL,

    /**
     * SMS notification (critical alerts only).
     */
    SMS;

    /**
     * Check if this channel requires user contact information.
     */
    public boolean requiresContactInfo() {
        return this == EMAIL || this == SMS || this == PUSH;
    }

    /**
     * Check if this channel is suitable for real-time delivery.
     */
    public boolean isRealTime() {
        return this == WEBSOCKET || this == PUSH;
    }

    /**
     * Check if this channel should be used for critical notifications only.
     */
    public boolean isCriticalOnly() {
        return this == SMS;
    }
}
