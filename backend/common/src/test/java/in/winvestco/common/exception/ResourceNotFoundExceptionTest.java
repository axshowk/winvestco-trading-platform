package in.winvestco.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_WithResourceNameAndIdentifier_ShouldSetFieldsCorrectly() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "123");

        assertEquals("User not found with identifier: 123", ex.getMessage());
        assertEquals("RESOURCE_NOT_FOUND", ex.getErrorCode());
        assertEquals("User not found", ex.getUserMessage());
    }

    @Test
    void constructor_WithMessage_ShouldSetFieldsCorrectly() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Custom error message");

        assertEquals("Custom error message", ex.getMessage());
        assertEquals("RESOURCE_NOT_FOUND", ex.getErrorCode());
        assertEquals("Custom error message", ex.getUserMessage());
    }
}
