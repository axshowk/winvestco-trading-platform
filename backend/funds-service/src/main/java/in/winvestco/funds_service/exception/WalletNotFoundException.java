package in.winvestco.funds_service.exception;

/**
 * Exception thrown when a wallet is not found
 */
public class WalletNotFoundException extends RuntimeException {
    
    public WalletNotFoundException(Long walletId) {
        super("Wallet not found with id: " + walletId);
    }
    
    public WalletNotFoundException(String field, Object value) {
        super("Wallet not found with " + field + ": " + value);
    }
}
