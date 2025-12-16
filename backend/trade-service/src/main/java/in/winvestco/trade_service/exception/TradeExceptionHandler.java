package in.winvestco.trade_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import in.winvestco.common.exception.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Trade-specific exception handler.
 * Handles exceptions specific to the trade-service domain.
 */
@Slf4j
@RestControllerAdvice
public class TradeExceptionHandler {

    @ExceptionHandler(TradeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTradeNotFound(TradeNotFoundException ex) {
        log.warn("Trade not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now().toString(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                null,
                "TRADE_NOT_FOUND",
                null,
                null);

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidTradeStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTradeState(InvalidTradeStateException ex) {
        log.warn("Invalid trade state: {}", ex.getMessage());

        Map<String, String> details = new HashMap<>();
        details.put("tradeId", ex.getTradeId());
        details.put("currentStatus", ex.getCurrentStatus().name());
        details.put("attemptedAction", ex.getAttemptedAction());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now().toString(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                null,
                "INVALID_TRADE_STATE",
                null,
                details);

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TradeValidationException.class)
    public ResponseEntity<ErrorResponse> handleTradeValidation(TradeValidationException ex) {
        log.warn("Trade validation failed: {}", ex.getMessage());

        Map<String, String> details = new HashMap<>();
        details.put("errorCode", ex.getErrorCode());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now().toString(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                null,
                "TRADE_VALIDATION_ERROR",
                null,
                details);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
