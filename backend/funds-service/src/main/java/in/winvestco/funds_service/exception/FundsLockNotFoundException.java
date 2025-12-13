package in.winvestco.funds_service.exception;

/**
 * Exception thrown when a funds lock is not found
 */
public class FundsLockNotFoundException extends RuntimeException {
    
    public FundsLockNotFoundException(String orderId) {
        super("Funds lock not found for order: " + orderId);
    }
    
    public FundsLockNotFoundException(Long lockId) {
        super("Funds lock not found with id: " + lockId);
    }
}
