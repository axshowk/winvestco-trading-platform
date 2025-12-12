package in.winvestco.common.exception;

/**
 * Base exception class for all business logic exceptions in the trading application.
 * All custom exceptions should extend this class for consistent error handling.
 */
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final String userMessage;

    protected BaseException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    protected BaseException(String errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
