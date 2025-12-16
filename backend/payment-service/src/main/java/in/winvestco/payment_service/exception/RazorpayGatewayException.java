package in.winvestco.payment_service.exception;

/**
 * Exception thrown when Razorpay gateway operations fail
 */
public class RazorpayGatewayException extends RuntimeException {
    
    private final String errorCode;

    public RazorpayGatewayException(String message) {
        super(message);
        this.errorCode = null;
    }

    public RazorpayGatewayException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public RazorpayGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public RazorpayGatewayException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
