package in.winvestco.portfolio_service.exception;

import in.winvestco.common.exception.ResourceNotFoundException;

/**
 * Exception thrown when a portfolio is not found
 */
public class PortfolioNotFoundException extends ResourceNotFoundException {

    public PortfolioNotFoundException(Long id) {
        super("Portfolio", String.valueOf(id));
    }

    public PortfolioNotFoundException(String field, Object value) {
        super("Portfolio", field + ": " + value);
    }
}

