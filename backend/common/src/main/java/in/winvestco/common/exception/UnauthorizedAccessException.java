package in.winvestco.common.exception;


/**
 * Exception thrown when access is denied due to insufficient permissions.
 */
public class UnauthorizedAccessException extends BaseException {

    public UnauthorizedAccessException(String message) {
        super("UNAUTHORIZED_ACCESS", message, message);
    }

    public UnauthorizedAccessException(String resource, String action) {
        super("UNAUTHORIZED_ACCESS",
              String.format("Access denied for %s: %s", resource, action),
              "Access denied");
    }
}
