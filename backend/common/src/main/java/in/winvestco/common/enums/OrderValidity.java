package in.winvestco.common.enums;

/**
 * Order validity/time in force for order expiration rules.
 */
public enum OrderValidity {
    DAY, // Expires at end of trading day (default)
    IOC, // Immediate or Cancel - fill immediately or cancel
    GTC // Good Till Cancelled - remains until manually cancelled
}
