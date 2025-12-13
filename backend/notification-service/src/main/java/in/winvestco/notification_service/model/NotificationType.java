package in.winvestco.notification_service.model;

/**
 * Types of notifications in the system.
 */
public enum NotificationType {
    // Order events
    ORDER_CREATED,
    ORDER_VALIDATED,
    ORDER_CANCELLED,
    ORDER_REJECTED,
    ORDER_EXPIRED,
    ORDER_PARTIALLY_FILLED,
    ORDER_FILLED,
    
    // Trade events
    TRADE_EXECUTED,
    
    // Funds events
    FUNDS_LOCKED,
    FUNDS_RELEASED,
    FUNDS_DEPOSITED,
    FUNDS_WITHDRAWN,
    
    // User events (cannot be muted - security)
    USER_LOGIN,
    USER_PASSWORD_CHANGED,
    USER_STATUS_CHANGED;

    /**
     * Check if this notification type can be muted by the user.
     * Security-related notifications cannot be muted.
     */
    public boolean isMutable() {
        return this != USER_LOGIN 
            && this != USER_PASSWORD_CHANGED 
            && this != USER_STATUS_CHANGED;
    }
}
