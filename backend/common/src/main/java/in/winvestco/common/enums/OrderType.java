package in.winvestco.common.enums;

/**
 * Order type for different execution strategies.
 */
public enum OrderType {
    MARKET, // Execute at current market price
    LIMIT, // Execute at specified price or better
    STOP_LOSS, // Trigger when price reaches stop price
    STOP_LIMIT // Trigger at stop price, then limit order
}
