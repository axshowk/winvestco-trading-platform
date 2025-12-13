package in.winvestco.funds_service.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when there are insufficient funds for an operation
 */
public class InsufficientFundsException extends RuntimeException {
    
    private final BigDecimal requested;
    private final BigDecimal available;
    
    public InsufficientFundsException(BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient funds: requested %.4f, available %.4f", requested, available));
        this.requested = requested;
        this.available = available;
    }
    
    public BigDecimal getRequested() {
        return requested;
    }
    
    public BigDecimal getAvailable() {
        return available;
    }
}
