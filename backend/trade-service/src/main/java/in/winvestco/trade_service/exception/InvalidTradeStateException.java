package in.winvestco.trade_service.exception;

import in.winvestco.common.enums.TradeStatus;

/**
 * Exception thrown when an invalid state transition is attempted.
 */
public class InvalidTradeStateException extends RuntimeException {
    
    private final String tradeId;
    private final TradeStatus currentStatus;
    private final String attemptedAction;

    public InvalidTradeStateException(String tradeId, TradeStatus currentStatus, String attemptedAction) {
        super(String.format("Cannot %s trade %s in state %s", attemptedAction, tradeId, currentStatus));
        this.tradeId = tradeId;
        this.currentStatus = currentStatus;
        this.attemptedAction = attemptedAction;
    }

    public String getTradeId() {
        return tradeId;
    }

    public TradeStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }
}
