package in.winvestco.common.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


import in.winvestco.common.util.LoggingUtils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Base exception handler that services can extend to add their specific exception handling.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseServiceExceptionHandler extends ResponseEntityExceptionHandler {


    protected final LoggingUtils loggingUtils;

    /**
     * Handle service-specific exceptions. Services should override this method
     * to handle their custom exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleServiceSpecificException(
            Exception ex, WebRequest request) {

        loggingUtils.setServiceName(getServiceName());
        loggingUtils.logError(getServiceName(), "handleServiceSpecificException", ex,
                             "errorType=service_specific_error",
                             "exceptionClass=" + ex.getClass().getSimpleName());

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SERVICE_ERROR",
                "An error occurred in " + getServiceName(),
                request.getDescription(false).replace("uri=", ""),
                null
        );

        loggingUtils.clearContext();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle ConstraintViolationException for Bean Validation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        loggingUtils.setServiceName(getServiceName());
        loggingUtils.logError(getServiceName(), "handleConstraintViolationException", ex,
                             "errorType=constraint_violation",
                             "violationCount=" + ex.getConstraintViolations().size());

        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(fieldName, message);
        }

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "CONSTRAINT_VIOLATION",
                "Validation failed for " + getServiceName(),
                request.getDescription(false).replace("uri=", ""),
                errors
        );

        loggingUtils.clearContext();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle validation errors for service-specific DTOs
     * This method can be called by other exception handlers to create consistent validation error responses
     */
    protected ResponseEntity<ErrorResponse> createValidationErrorResponse(
            MethodArgumentNotValidException ex, WebRequest request) {

        loggingUtils.setServiceName(getServiceName());
        loggingUtils.logError(getServiceName(), "createValidationErrorResponse", ex,
                             "errorType=validation_error",
                             "fieldCount=" + ex.getBindingResult().getFieldErrorCount());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Validation failed for " + getServiceName(),
                request.getDescription(false).replace("uri=", ""),
                errors
        );

        loggingUtils.clearContext();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Get the service name for logging purposes. Each service should override this.
     */
    protected abstract String getServiceName();

    /**
     * Create a standardized error response
     */
    protected ErrorResponse createErrorResponse(HttpStatus status, String errorCode,
                                              String message, String path, Map<String, String> details) {
        String requestId = loggingUtils.generateRequestId();
        String timestamp = LocalDateTime.now().toString();

        return new ErrorResponse(
                timestamp,
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                errorCode,
                requestId,
                details
        );
    }
}
