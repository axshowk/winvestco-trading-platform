package in.winvestco.portfolio_service.exception;

import in.winvestco.common.exception.ResourceNotFoundException;

/**
 * Exception thrown when a holding is not found
 */
public class HoldingNotFoundException extends ResourceNotFoundException {

    public HoldingNotFoundException(Long id) {
        super("Holding", String.valueOf(id));
    }

    public HoldingNotFoundException(String field, Object value) {
        super("Holding", field + ": " + value);
    }
}

