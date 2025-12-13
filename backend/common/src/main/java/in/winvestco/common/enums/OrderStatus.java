package in.winvestco.common.enums;

/**
 * Order status representing the lifecycle states of an order.
 */
public enum OrderStatus {
    NEW, // Order just created
    VALIDATED, // Order passed validation
    FUNDS_LOCKED, // Funds locked by funds-service
    PENDING, // Waiting for execution
    PARTIALLY_FILLED, // Partially filled
    FILLED, // Completely filled
    CANCELLED, // Cancelled by user
    REJECTED, // Rejected by system/validation
    EXPIRED // Order expired (EOD for DAY orders)
}
