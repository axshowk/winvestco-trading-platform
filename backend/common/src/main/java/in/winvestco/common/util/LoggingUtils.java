package in.winvestco.common.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Centralized logging utilities for consistent logging across all services.
 * Provides methods for structured logging, request tracing, and common log patterns.
 */
@Slf4j
@Component
public class LoggingUtils {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String SERVICE_NAME_KEY = "serviceName";

    /**
     * Generate and set a unique request ID for tracing across services
     */
    public String generateRequestId() {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_KEY, requestId);
        return requestId;
    }

    /**
     * Set the current user ID in the MDC for logging context
     */
    public void setUserId(String userId) {
        MDC.put(USER_ID_KEY, userId);
    }

    /**
     * Set the service name in the MDC for logging context
     */
    public void setServiceName(String serviceName) {
        MDC.put(SERVICE_NAME_KEY, serviceName);
    }

    /**
     * Clear all MDC context
     */
    public void clearContext() {
        MDC.clear();
    }

    /**
     * Log the start of a service operation (DEBUG level)
     */
    public void logServiceStart(String serviceName, String operation, Object... params) {
        setServiceName(serviceName);
        if (log.isDebugEnabled()) {
            log.debug("Service operation started: {} - {} with params: {}", serviceName, operation, params);
        }
    }

    /**
     * Log the completion of a service operation (INFO level)
     */
    public void logServiceEnd(String serviceName, String operation, Object... params) {
        setServiceName(serviceName);
        log.info("Service operation completed: {} - {} with result: {}", serviceName, operation, params);
    }

    /**
     * Log an error with context
     */
    public void logError(String serviceName, String operation, Throwable error, Object... params) {
        setServiceName(serviceName);
        log.error("Service operation failed: {} - {} with params: {} - Error: {}",
                  serviceName, operation, params, error.getMessage(), error);
    }

    /**
     * Log debug information with structured data
     */
    public void logDebug(String message, Map<String, Object> context) {
        if (log.isDebugEnabled()) {
            String contextStr = context != null ? context.toString() : "";
            log.debug("{} - Context: {}", message, contextStr);
        }
    }

    /**
     * Log debug information with simple message
     */
    public void logDebug(String serviceName, String operation, Object... params) {
        setServiceName(serviceName);
        if (log.isDebugEnabled()) {
            log.debug("DEBUG: {} - {} with params: {}", serviceName, operation, params);
        }
    }

    /**
     * Log info level message
     */
    public void logInfo(String serviceName, String operation, Object... params) {
        setServiceName(serviceName);
        log.info("INFO: {} - {} with params: {}", serviceName, operation, params);
    }

    /**
     * Log warning message
     */
    public void logWarn(String serviceName, String operation, Object... params) {
        setServiceName(serviceName);
        log.warn("WARN: {} - {} with params: {}", serviceName, operation, params);
    }

    /**
     * Log error message only
     */
    public void logError(String serviceName, String operation, String message, Object... params) {
        setServiceName(serviceName);
        log.error("ERROR: {} - {}: {} with params: {}", serviceName, operation, message, params);
    }

    /**
     * Log performance metrics with appropriate levels based on duration
     */
    public void logPerformance(String operation, long startTime, long endTime) {
        long duration = endTime - startTime;

        if (duration > 10000) { // Log as ERROR for operations taking more than 10 seconds
            log.error("SLOW OPERATION: {} completed in {} ms", operation, duration);
        } else if (duration > 5000) { // Log as WARN for operations taking more than 5 seconds
            log.warn("Slow operation detected: {} took {} ms", operation, duration);
        } else if (duration > 1000) { // Log as INFO for operations taking more than 1 second
            log.info("Performance: {} completed in {} ms", operation, duration);
        } else { // Log as DEBUG for fast operations
            if (log.isDebugEnabled()) {
                log.debug("Performance: {} completed in {} ms", operation, duration);
            }
        }
    }
}
