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
}
