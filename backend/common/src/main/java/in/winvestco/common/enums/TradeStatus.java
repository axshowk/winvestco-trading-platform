package in.winvestco.common.enums;

/**
 * Trade status representing the lifecycle states of a trade.
 * 
 * State Machine:
 * CREATED → VALIDATED → PLACED → EXECUTING → FILLED → CLOSED
 * ↘ PARTIALLY_FILLED ↗
 * Any state can transition to CANCELLED or FAILED based on user action or
 * system error.
 */
public enum TradeStatus {
    CREATED, // Trade intent received from order
    VALIDATED, // Trade passed validation
    PLACED, // Sent to execution engine
    EXECUTING, // Execution in progress
    PARTIALLY_FILLED, // Some quantity executed
    FILLED, // Fully executed
    CLOSED, // Trade settled
    CANCELLED, // User cancelled
    FAILED // System/validation failure
}
