package in.winvestco.common.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import in.winvestco.common.util.LoggingUtils;

import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized global exception handler for all services.
 * This provides consistent error handling across the entire application.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

        private final LoggingUtils loggingUtils;

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex, WebRequest request) {

                loggingUtils.setServiceName("GlobalExceptionHandler");
                loggingUtils.logError("GlobalExceptionHandler", "handleMethodArgumentNotValid", ex,
                                "errorType=validation_error",
                                "fieldErrorCount=" + ex.getBindingResult().getFieldErrorCount());

                Map<String, String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(
                                                org.springframework.validation.FieldError::getField,
                                                fieldError -> fieldError.getDefaultMessage() != null
                                                                ? fieldError.getDefaultMessage()
                                                                : "Invalid value",
                                                (existing, replacement) -> existing + ", " + replacement));

                ErrorResponse errorResponse = createErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "VALIDATION_ERROR",
                                "Validation failed",
                                request.getDescription(false).replace("uri=", ""),
                                errors);

                loggingUtils.clearContext();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex, WebRequest request) {

                loggingUtils.setServiceName("GlobalExceptionHandler");
                loggingUtils.logError("GlobalExceptionHandler", "handleResourceNotFound", ex,
                                "errorCode=" + ex.getErrorCode(),
                                "message=" + ex.getMessage());

                ErrorResponse errorResponse = createErrorResponse(
                                HttpStatus.NOT_FOUND,
                                ex.getErrorCode(),
                                ex.getUserMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                null);

                loggingUtils.clearContext();
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(UnauthorizedAccessException.class)
        public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
                        UnauthorizedAccessException ex, WebRequest request) {

                loggingUtils.setServiceName("GlobalExceptionHandler");
                loggingUtils.logError("GlobalExceptionHandler", "handleUnauthorizedAccess", ex,
                                "errorCode=" + ex.getErrorCode(),
                                "message=" + ex.getMessage());

                ErrorResponse errorResponse = createErrorResponse(
                                HttpStatus.FORBIDDEN,
                                ex.getErrorCode(),
                                ex.getUserMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                null);

                loggingUtils.clearContext();
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(BusinessValidationException.class)
        public ResponseEntity<ErrorResponse> handleBusinessValidation(
                        BusinessValidationException ex, WebRequest request) {

                loggingUtils.setServiceName("GlobalExceptionHandler");
                loggingUtils.logError("GlobalExceptionHandler", "handleBusinessValidation", ex,
                                "errorCode=" + ex.getErrorCode(),
                                "message=" + ex.getMessage());

                ErrorResponse errorResponse = createErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getErrorCode(),
                                ex.getUserMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                null);

                loggingUtils.clearContext();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex, WebRequest request) {

                loggingUtils.setServiceName("GlobalExceptionHandler");
                loggingUtils.logError("GlobalExceptionHandler", "handleConstraintViolation", ex,
                                "errorType=validation_error",
                                "violationCount=" + ex.getConstraintViolations().size());

                Map<String, String> errors = new HashMap<>();
                ex.getConstraintViolations().forEach(violation -> errors.put(violation.getPropertyPath().toString(),
                                violation.getMessage()));

                ErrorResponse errorResponse = createErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "VALIDATION_ERROR",
                                "Validation failed",
                                request.getDescription(false).replace("uri=", ""),
                                errors);

                loggingUtils.clearContext();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex, WebRequest request) {

                loggingUtils.setServiceName("GlobalExceptionHandler");
                loggingUtils.logError("GlobalExceptionHandler", "handleIllegalArgument", ex,
                                "errorType=illegal_argument");

                ErrorResponse errorResponse = createErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "ILLEGAL_ARGUMENT",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                null);

                loggingUtils.clearContext();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleIllegalState(
                        IllegalStateException ex, WebRequest request) {

                loggingUtils.setServiceName("GlobalExceptionHandler");
                loggingUtils.logError("GlobalExceptionHandler", "handleIllegalState", ex,
                                "errorType=illegal_state");

                ErrorResponse errorResponse = createErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                "ILLEGAL_STATE",
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                null);

                loggingUtils.clearContext();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleAllExceptions(
                        Exception ex, WebRequest request) {

                loggingUtils.setServiceName("GlobalExceptionHandler");
                loggingUtils.logError("GlobalExceptionHandler", "handleAllExceptions", ex,
                                "errorType=unexpected_error",
                                "exceptionClass=" + ex.getClass().getSimpleName());

                ErrorResponse errorResponse = createErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "INTERNAL_SERVER_ERROR",
                                "An unexpected error occurred",
                                request.getDescription(false).replace("uri=", ""),
                                null);

                loggingUtils.clearContext();
                return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        /**
         * Create a standardized error response
         */
        private ErrorResponse createErrorResponse(HttpStatus status, String errorCode,
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
                                details);
        }
}
