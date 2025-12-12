package in.winvestco.user_service.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import in.winvestco.common.exception.BaseServiceExceptionHandler;
import in.winvestco.common.util.LoggingUtils;

/**
 * User service specific exception handler that extends the base exception handler.
 * This provides consistent error handling while allowing service-specific customizations.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseServiceExceptionHandler {

    public GlobalExceptionHandler(LoggingUtils loggingUtils) {
        super(loggingUtils);
    }

    @Override
    protected String getServiceName() {
        return "UserService";
    }
}
