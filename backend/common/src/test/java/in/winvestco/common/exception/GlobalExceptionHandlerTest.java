package in.winvestco.common.exception;

import in.winvestco.common.util.LoggingUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Mock
    private LoggingUtils loggingUtils;

    @Mock
    private WebRequest webRequest;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(webRequest.getDescription(false)).thenReturn("uri=/test/path");
        when(loggingUtils.generateRequestId()).thenReturn("test-request-id");
    }

    @Test
    void handleMethodArgumentNotValid_ShouldReturnBadRequestWithDetails() throws Exception {
        // Arrange
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "email", "Email is required"));
        bindingResult.addError(new FieldError("testObject", "password", "Password is required"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                (MethodParameter) null, bindingResult);

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("VALIDATION_ERROR", body.getErrorCode());
        assertEquals("Validation failed", body.getMessage());
        assertEquals("/test/path", body.getPath());

        Map<String, String> details = body.getDetails();
        assertNotNull(details);
        assertEquals("Email is required", details.get("email"));
        assertEquals("Password is required", details.get("password"));
    }

    @Test
    void handleResourceNotFound_ShouldReturnNotFound() {
        // Arrange
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "123");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFound(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getStatus());
        assertEquals("RESOURCE_NOT_FOUND", body.getErrorCode());
        assertEquals("User not found with identifier: 123", body.getMessage());
    }

    @Test
    void handleUnauthorizedAccess_ShouldReturnForbidden() {
        // Arrange
        UnauthorizedAccessException ex = new UnauthorizedAccessException("Access denied", "ACCESS_DENIED");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnauthorizedAccess(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(403, body.getStatus());
        assertEquals("ACCESS_DENIED", body.getErrorCode());
    }

    @Test
    void handleAllExceptions_ShouldReturnInternalServerError() {
        // Arrange
        Exception ex = new RuntimeException("Unexpected error");

        // Act
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAllExceptions(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", body.getErrorCode());
    }
}
