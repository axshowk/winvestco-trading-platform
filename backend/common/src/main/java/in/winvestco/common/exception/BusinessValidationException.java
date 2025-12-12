package in.winvestco.common.exception;



/**
 * Exception thrown when a business rule validation fails.
 */
public class BusinessValidationException extends BaseException {

    public BusinessValidationException(String message) {
        super("BUSINESS_VALIDATION_ERROR", message, message);
    }

    public BusinessValidationException(String message, String userMessage) {
        super("BUSINESS_VALIDATION_ERROR", message, userMessage);
    }
}
