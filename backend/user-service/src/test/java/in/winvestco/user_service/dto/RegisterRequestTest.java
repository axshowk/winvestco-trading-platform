package in.winvestco.user_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegisterRequestTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void whenAllFieldsAreValid_thenNoViolations() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPassword("Password123!");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    public void whenPasswordIsTooShort_thenViolation() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPassword("P123!"); // 5 chars

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Password must be at least 8 characters long", violations.iterator().next().getMessage());
    }

    @Test
    public void whenPasswordHasNoNumber_thenViolation() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPassword("Password!");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Password must contain at least one number", violations.iterator().next().getMessage());
    }

    @Test
    public void whenPasswordHasNoSpecialChar_thenViolation() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPassword("Password123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("Password must contain at least one special character", violations.iterator().next().getMessage());
    }
}
