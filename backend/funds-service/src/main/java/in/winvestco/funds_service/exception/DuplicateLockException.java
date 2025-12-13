package in.winvestco.funds_service.exception;

/**
 * Exception thrown when attempting to create a duplicate lock for an order
 */
public class DuplicateLockException extends RuntimeException {
    
    public DuplicateLockException(String orderId) {
        super("Funds lock already exists for order: " + orderId);
    }
}
