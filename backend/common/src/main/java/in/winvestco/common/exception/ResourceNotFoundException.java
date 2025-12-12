package in.winvestco.common.exception;


/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceName, String identifier) {
        super("RESOURCE_NOT_FOUND",
              String.format("%s not found with identifier: %s", resourceName, identifier),
              String.format("%s not found", resourceName));
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, message);
    }
}
