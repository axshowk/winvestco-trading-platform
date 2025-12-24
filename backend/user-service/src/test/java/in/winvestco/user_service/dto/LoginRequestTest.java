package in.winvestco.user_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LoginRequest DTO
 * Tests validation constraints and record functionality
 */
@DisplayName("LoginRequest Tests")
class LoginRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Valid Request Tests")
    class ValidRequestTests {

        @Test
        @DisplayName("Should pass validation with valid email and password")
        void validate_WithValidRequest_ShouldHaveNoViolations() {
            LoginRequest request = new LoginRequest("test@example.com", "password123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass validation with complex email")
        void validate_WithComplexEmail_ShouldHaveNoViolations() {
            LoginRequest request = new LoginRequest("user.name+tag@subdomain.example.com", "password123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should fail validation when email is null")
        void validate_WithNullEmail_ShouldHaveViolation() {
            LoginRequest request = new LoginRequest(null, "password123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
            assertThat(violations)
                    .anyMatch(v -> v.getMessage().contains("required") || v.getMessage().contains("blank"));
        }

        @Test
        @DisplayName("Should fail validation when email is empty")
        void validate_WithEmptyEmail_ShouldHaveViolation() {
            LoginRequest request = new LoginRequest("", "password123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation when email is blank")
        void validate_WithBlankEmail_ShouldHaveViolation() {
            LoginRequest request = new LoginRequest("   ", "password123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation with invalid email format")
        void validate_WithInvalidEmailFormat_ShouldHaveViolation() {
            LoginRequest request = new LoginRequest("not-an-email", "password123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().toLowerCase().contains("email"));
        }

        @Test
        @DisplayName("Should fail validation with email missing @")
        void validate_WithEmailMissingAt_ShouldHaveViolation() {
            LoginRequest request = new LoginRequest("testexample.com", "password123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation with email missing domain")
        void validate_WithEmailMissingDomain_ShouldHaveViolation() {
            LoginRequest request = new LoginRequest("test@", "password123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should fail validation when password is null")
        void validate_WithNullPassword_ShouldHaveViolation() {
            LoginRequest request = new LoginRequest("test@example.com", null);

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
            assertThat(violations)
                    .anyMatch(v -> v.getMessage().contains("required") || v.getMessage().contains("blank"));
        }

        @Test
        @DisplayName("Should fail validation when password is empty")
        void validate_WithEmptyPassword_ShouldHaveViolation() {
            LoginRequest request = new LoginRequest("test@example.com", "");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail validation when password is blank")
        void validate_WithBlankPassword_ShouldHaveViolation() {
            LoginRequest request = new LoginRequest("test@example.com", "   ");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should pass validation with short password")
        void validate_WithShortPassword_ShouldPass() {
            // No minimum password length constraint on LoginRequest
            LoginRequest request = new LoginRequest("test@example.com", "a");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Record Accessor Tests")
    class RecordAccessorTests {

        @Test
        @DisplayName("Should return email via accessor")
        void email_ShouldReturnEmail() {
            LoginRequest request = new LoginRequest("test@example.com", "password123");

            assertThat(request.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should return password via accessor")
        void password_ShouldReturnPassword() {
            LoginRequest request = new LoginRequest("test@example.com", "password123");

            assertThat(request.password()).isEqualTo("password123");
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same values")
        void equals_WithSameValues_ShouldReturnTrue() {
            LoginRequest request1 = new LoginRequest("test@example.com", "password123");
            LoginRequest request2 = new LoginRequest("test@example.com", "password123");

            assertThat(request1).isEqualTo(request2);
        }

        @Test
        @DisplayName("Should not be equal when different email")
        void equals_WithDifferentEmail_ShouldReturnFalse() {
            LoginRequest request1 = new LoginRequest("test1@example.com", "password123");
            LoginRequest request2 = new LoginRequest("test2@example.com", "password123");

            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should not be equal when different password")
        void equals_WithDifferentPassword_ShouldReturnFalse() {
            LoginRequest request1 = new LoginRequest("test@example.com", "password123");
            LoginRequest request2 = new LoginRequest("test@example.com", "different");

            assertThat(request1).isNotEqualTo(request2);
        }

        @Test
        @DisplayName("Should have consistent hashCode")
        void hashCode_WithSameValues_ShouldBeEqual() {
            LoginRequest request1 = new LoginRequest("test@example.com", "password123");
            LoginRequest request2 = new LoginRequest("test@example.com", "password123");

            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return record string representation")
        void toString_ShouldReturnRecordFormat() {
            LoginRequest request = new LoginRequest("test@example.com", "password123");

            String str = request.toString();

            assertThat(str).contains("LoginRequest");
            assertThat(str).contains("test@example.com");
        }
    }
}
