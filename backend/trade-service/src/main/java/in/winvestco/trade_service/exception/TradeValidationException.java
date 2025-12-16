package in.winvestco.trade_service.exception;

/**
 * Exception thrown when trade validation fails.
 */
public class TradeValidationException extends RuntimeException {
    
    private final String errorCode;

    public TradeValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
    }

    public TradeValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TradeValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "VALIDATION_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
