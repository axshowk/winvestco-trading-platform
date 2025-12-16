package in.winvestco.trade_service.exception;

/**
 * Exception thrown when a trade is not found.
 */
public class TradeNotFoundException extends RuntimeException {
    
    public TradeNotFoundException(String tradeId) {
        super("Trade not found: " + tradeId);
    }

    public TradeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
