package in.winvestco.user_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserNotFoundException
 * Tests exception construction and message formatting
 */
@DisplayName("UserNotFoundException Tests")
class UserNotFoundExceptionTest {

    @Nested
    @DisplayName("Constructor with Long ID Tests")
    class ConstructorWithLongIdTests {

        @Test
        @DisplayName("Should create exception with user id message")
        void constructor_WithLongId_ShouldSetMessage() {
            Long userId = 123L;

            UserNotFoundException exception = new UserNotFoundException(userId);

            assertThat(exception.getMessage()).contains("User not found with id: 123");
        }

        @Test
        @DisplayName("Should set error code to USER_NOT_FOUND")
        void constructor_WithLongId_ShouldSetErrorCode() {
            UserNotFoundException exception = new UserNotFoundException(1L);

            assertThat(exception.getErrorCode()).isEqualTo("USER_NOT_FOUND");
        }

        @Test
        @DisplayName("Should handle large user id")
        void constructor_WithLargeId_ShouldSetMessage() {
            Long userId = 999999999L;

            UserNotFoundException exception = new UserNotFoundException(userId);

            assertThat(exception.getMessage()).contains("999999999");
        }
    }

    @Nested
    @DisplayName("Constructor with String Login Tests")
    class ConstructorWithStringLoginTests {

        @Test
        @DisplayName("Should create exception with email message")
        void constructor_WithEmail_ShouldSetMessage() {
            String email = "test@example.com";

            UserNotFoundException exception = new UserNotFoundException(email);

            assertThat(exception.getMessage()).contains("User not found: test@example.com");
        }

        @Test
        @DisplayName("Should set error code to USER_NOT_FOUND")
        void constructor_WithEmail_ShouldSetErrorCode() {
            UserNotFoundException exception = new UserNotFoundException("test@example.com");

            assertThat(exception.getErrorCode()).isEqualTo("USER_NOT_FOUND");
        }

        @Test
        @DisplayName("Should handle username instead of email")
        void constructor_WithUsername_ShouldSetMessage() {
            String username = "john_doe";

            UserNotFoundException exception = new UserNotFoundException(username);

            assertThat(exception.getMessage()).contains("User not found: john_doe");
        }

        @Test
        @DisplayName("Should handle empty string")
        void constructor_WithEmptyString_ShouldSetMessage() {
            UserNotFoundException exception = new UserNotFoundException("");

            assertThat(exception.getMessage()).contains("User not found:");
        }
    }

    @Nested
    @DisplayName("Exception Hierarchy Tests")
    class ExceptionHierarchyTests {

        @Test
        @DisplayName("Should extend BaseException")
        void exception_ShouldExtendBaseException() {
            UserNotFoundException exception = new UserNotFoundException(1L);

            assertThat(exception).isInstanceOf(in.winvestco.common.exception.BaseException.class);
        }

        @Test
        @DisplayName("Should be throwable as RuntimeException")
        void exception_ShouldBeRuntimeException() {
            UserNotFoundException exception = new UserNotFoundException(1L);

            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }
}
