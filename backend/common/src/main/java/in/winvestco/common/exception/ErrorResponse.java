package in.winvestco.common.exception;

import java.util.Map;

/**
 * Standard error response structure for consistent API error responses across all services.
 */
public class ErrorResponse {
    private final String timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String errorCode;
    private final String requestId;
    private final Map<String, String> details;

    public ErrorResponse(String timestamp, int status, String error, String message,
                        String path, String errorCode, String requestId, Map<String, String> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.errorCode = errorCode;
        this.requestId = requestId;
        this.details = details;
    }

    public ErrorResponse(String timestamp, int status, String error, String message,
                        String path, String errorCode, String requestId) {
        this(timestamp, status, error, message, path, errorCode, requestId, null);
    }

    // Getters
    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public String getErrorCode() { return errorCode; }
    public String getRequestId() { return requestId; }
    public Map<String, String> getDetails() { return details; }
}
