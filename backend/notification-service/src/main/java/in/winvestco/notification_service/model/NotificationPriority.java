package in.winvestco.notification_service.model;

/**
 * Priority levels for notifications affecting delivery channels and timing.
 */
public enum NotificationPriority {
    /**
     * Critical notifications (margin calls, security breaches).
     * Sent via all enabled channels immediately, bypasses rate limits.
     */
    CRITICAL(0, true, true, true, true),

    /**
     * High priority notifications (order filled, large transactions).
     * Sent via WebSocket and Push immediately.
     */
    HIGH(1, true, true, false, false),

    /**
     * Medium priority notifications (order created, deposits).
     * Sent via WebSocket, email is optional.
     */
    MEDIUM(2, true, false, true, false),

    /**
     * Low priority notifications (price alerts, news).
     * Can be batched and sent in digests.
     */
    LOW(3, true, false, false, false);

    private final int order;
    private final boolean websocketImmediate;
    private final boolean pushImmediate;
    private final boolean emailEnabled;
    private final boolean smsEnabled;

    NotificationPriority(int order, boolean websocketImmediate, boolean pushImmediate,
            boolean emailEnabled, boolean smsEnabled) {
        this.order = order;
        this.websocketImmediate = websocketImmediate;
        this.pushImmediate = pushImmediate;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
    }

    public int getOrder() {
        return order;
    }

    public boolean isWebsocketImmediate() {
        return websocketImmediate;
    }

    public boolean isPushImmediate() {
        return pushImmediate;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    /**
     * Check if this priority level can bypass rate limits.
     */
    public boolean bypassesRateLimits() {
        return this == CRITICAL;
    }

    /**
     * Check if notifications of this priority can be batched.
     */
    public boolean isBatchable() {
        return this == LOW || this == MEDIUM;
    }
}
