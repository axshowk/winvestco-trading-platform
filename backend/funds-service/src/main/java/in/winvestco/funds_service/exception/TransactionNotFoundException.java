package in.winvestco.funds_service.exception;

/**
 * Exception thrown when a transaction is not found
 */
public class TransactionNotFoundException extends RuntimeException {
    
    public TransactionNotFoundException(Long transactionId) {
        super("Transaction not found with id: " + transactionId);
    }
    
    public TransactionNotFoundException(String field, Object value) {
        super("Transaction not found with " + field + ": " + value);
    }
}
