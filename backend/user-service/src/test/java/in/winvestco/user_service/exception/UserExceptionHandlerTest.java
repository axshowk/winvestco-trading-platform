package in.winvestco.user_service.exception;

import in.winvestco.common.util.LoggingUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserExceptionHandler
 * Tests exception handler configuration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserExceptionHandler Tests")
class UserExceptionHandlerTest {

    @Mock
    private LoggingUtils loggingUtils;

    @Test
    @DisplayName("Should return correct service name")
    void getServiceName_ShouldReturnUserService() {
        UserExceptionHandler handler = new UserExceptionHandler(loggingUtils);

        // Use reflection to access protected method
        String serviceName = handler.getServiceName();

        assertThat(serviceName).isEqualTo("UserService");
    }

    @Test
    @DisplayName("Should extend BaseServiceExceptionHandler")
    void handler_ShouldExtendBaseServiceExceptionHandler() {
        UserExceptionHandler handler = new UserExceptionHandler(loggingUtils);

        assertThat(handler).isInstanceOf(in.winvestco.common.exception.BaseServiceExceptionHandler.class);
    }

    @Test
    @DisplayName("Should be annotated with RestControllerAdvice")
    void handler_ShouldHaveRestControllerAdviceAnnotation() {
        boolean hasAnnotation = UserExceptionHandler.class.isAnnotationPresent(
                org.springframework.web.bind.annotation.RestControllerAdvice.class);

        assertThat(hasAnnotation).isTrue();
    }

    @Test
    @DisplayName("Should accept LoggingUtils in constructor")
    void constructor_WithLoggingUtils_ShouldCreateHandler() {
        UserExceptionHandler handler = new UserExceptionHandler(loggingUtils);

        assertThat(handler).isNotNull();
    }
}
