package in.winvestco.user_service.service;

import in.winvestco.common.event.*;
import in.winvestco.common.util.LoggingUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for UserEventPublisher
 * Tests RabbitMQ event publishing for user events
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventPublisher Tests")
class UserEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private LoggingUtils loggingUtils;

    private UserEventPublisher userEventPublisher;

    private static final String TEST_EXCHANGE = "user.exchange";
    private static final String USER_CREATED_ROUTING_KEY = "user.created";
    private static final String USER_UPDATED_ROUTING_KEY = "user.updated";
    private static final String USER_STATUS_CHANGED_ROUTING_KEY = "user.status.changed";
    private static final String USER_ROLE_CHANGED_ROUTING_KEY = "user.role.changed";
    private static final String USER_PASSWORD_CHANGED_ROUTING_KEY = "user.password.changed";
    private static final String USER_LOGIN_ROUTING_KEY = "user.login";

    @BeforeEach
    void setUp() {
        userEventPublisher = new UserEventPublisher(rabbitTemplate, loggingUtils);
        ReflectionTestUtils.setField(userEventPublisher, "exchange", TEST_EXCHANGE);
        ReflectionTestUtils.setField(userEventPublisher, "userCreatedRoutingKey", USER_CREATED_ROUTING_KEY);
        ReflectionTestUtils.setField(userEventPublisher, "userUpdatedRoutingKey", USER_UPDATED_ROUTING_KEY);
        ReflectionTestUtils.setField(userEventPublisher, "userStatusChangedRoutingKey",
                USER_STATUS_CHANGED_ROUTING_KEY);
        ReflectionTestUtils.setField(userEventPublisher, "userRoleChangedRoutingKey", USER_ROLE_CHANGED_ROUTING_KEY);
        ReflectionTestUtils.setField(userEventPublisher, "userPasswordChangedRoutingKey",
                USER_PASSWORD_CHANGED_ROUTING_KEY);
        ReflectionTestUtils.setField(userEventPublisher, "userLoginRoutingKey", USER_LOGIN_ROUTING_KEY);
    }

    @Nested
    @DisplayName("Publish User Created Tests")
    class PublishUserCreatedTests {

        @Test
        @DisplayName("Should publish user created event to RabbitMQ")
        void publishUserCreated_ShouldSendMessageToExchange() {
            UserCreatedEvent event = UserCreatedEvent.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            userEventPublisher.publishUserCreated(event);

            verify(rabbitTemplate).convertAndSend(eq(TEST_EXCHANGE), eq(USER_CREATED_ROUTING_KEY), eq(event));
        }

        @Test
        @DisplayName("Should log service start and end for user created")
        void publishUserCreated_ShouldLogServiceStartAndEnd() {
            UserCreatedEvent event = UserCreatedEvent.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .build();

            userEventPublisher.publishUserCreated(event);

            verify(loggingUtils).logServiceStart(eq("UserEventPublisher"), eq("publishUserCreated"), any());
            verify(loggingUtils).logServiceEnd(eq("UserEventPublisher"), eq("publishUserCreated"), any());
        }
    }

    @Nested
    @DisplayName("Publish User Updated Tests")
    class PublishUserUpdatedTests {

        @Test
        @DisplayName("Should publish user updated event to RabbitMQ")
        void publishUserUpdated_ShouldSendMessageToExchange() {
            UserUpdatedEvent event = UserUpdatedEvent.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .oldEmail("old@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .updatedAt(LocalDateTime.now())
                    .build();

            userEventPublisher.publishUserUpdated(event);

            verify(rabbitTemplate).convertAndSend(eq(TEST_EXCHANGE), eq(USER_UPDATED_ROUTING_KEY), eq(event));
        }

        @Test
        @DisplayName("Should log service start and end for user updated")
        void publishUserUpdated_ShouldLogServiceStartAndEnd() {
            UserUpdatedEvent event = UserUpdatedEvent.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .build();

            userEventPublisher.publishUserUpdated(event);

            verify(loggingUtils).logServiceStart(eq("UserEventPublisher"), eq("publishUserUpdated"), any());
            verify(loggingUtils).logServiceEnd(eq("UserEventPublisher"), eq("publishUserUpdated"), any());
        }
    }

    @Nested
    @DisplayName("Publish User Status Changed Tests")
    class PublishUserStatusChangedTests {

        @Test
        @DisplayName("Should publish user status changed event to RabbitMQ")
        void publishUserStatusChanged_ShouldSendMessageToExchange() {
            UserStatusChangedEvent event = UserStatusChangedEvent.builder()
                    .userId(1L)
                    .oldStatus(in.winvestco.common.enums.AccountStatus.ACTIVE)
                    .newStatus(in.winvestco.common.enums.AccountStatus.SUSPENDED)
                    .changedAt(LocalDateTime.now())
                    .build();

            userEventPublisher.publishUserStatusChanged(event);

            verify(rabbitTemplate).convertAndSend(eq(TEST_EXCHANGE), eq(USER_STATUS_CHANGED_ROUTING_KEY), eq(event));
        }

        @Test
        @DisplayName("Should log service start and end for status changed")
        void publishUserStatusChanged_ShouldLogServiceStartAndEnd() {
            UserStatusChangedEvent event = UserStatusChangedEvent.builder()
                    .userId(1L)
                    .build();

            userEventPublisher.publishUserStatusChanged(event);

            verify(loggingUtils).logServiceStart(eq("UserEventPublisher"), eq("publishUserStatusChanged"), any());
            verify(loggingUtils).logServiceEnd(eq("UserEventPublisher"), eq("publishUserStatusChanged"), any());
        }
    }

    @Nested
    @DisplayName("Publish User Role Changed Tests")
    class PublishUserRoleChangedTests {

        @Test
        @DisplayName("Should publish user role changed event to RabbitMQ")
        void publishUserRoleChanged_ShouldSendMessageToExchange() {
            UserRoleChangedEvent event = UserRoleChangedEvent.builder()
                    .userId(1L)
                    .oldRoles(java.util.Set.of(in.winvestco.common.enums.Role.USER))
                    .newRoles(
                            java.util.Set.of(in.winvestco.common.enums.Role.USER, in.winvestco.common.enums.Role.ADMIN))
                    .changedAt(LocalDateTime.now())
                    .build();

            userEventPublisher.publishUserRoleChanged(event);

            verify(rabbitTemplate).convertAndSend(eq(TEST_EXCHANGE), eq(USER_ROLE_CHANGED_ROUTING_KEY), eq(event));
        }

        @Test
        @DisplayName("Should log service start and end for role changed")
        void publishUserRoleChanged_ShouldLogServiceStartAndEnd() {
            UserRoleChangedEvent event = UserRoleChangedEvent.builder()
                    .userId(1L)
                    .build();

            userEventPublisher.publishUserRoleChanged(event);

            verify(loggingUtils).logServiceStart(eq("UserEventPublisher"), eq("publishUserRoleChanged"), any());
            verify(loggingUtils).logServiceEnd(eq("UserEventPublisher"), eq("publishUserRoleChanged"), any());
        }
    }

    @Nested
    @DisplayName("Publish User Password Changed Tests")
    class PublishUserPasswordChangedTests {

        @Test
        @DisplayName("Should publish user password changed event to RabbitMQ")
        void publishUserPasswordChanged_ShouldSendMessageToExchange() {
            UserPasswordChangedEvent event = UserPasswordChangedEvent.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .changedAt(LocalDateTime.now())
                    .ipAddress("127.0.0.1")
                    .userAgent("TestAgent")
                    .build();

            userEventPublisher.publishUserPasswordChanged(event);

            verify(rabbitTemplate).convertAndSend(eq(TEST_EXCHANGE), eq(USER_PASSWORD_CHANGED_ROUTING_KEY), eq(event));
        }

        @Test
        @DisplayName("Should log service start and end for password changed")
        void publishUserPasswordChanged_ShouldLogServiceStartAndEnd() {
            UserPasswordChangedEvent event = UserPasswordChangedEvent.builder()
                    .userId(1L)
                    .build();

            userEventPublisher.publishUserPasswordChanged(event);

            verify(loggingUtils).logServiceStart(eq("UserEventPublisher"), eq("publishUserPasswordChanged"), any());
            verify(loggingUtils).logServiceEnd(eq("UserEventPublisher"), eq("publishUserPasswordChanged"), any());
        }
    }

    @Nested
    @DisplayName("Publish User Login Tests")
    class PublishUserLoginTests {

        @Test
        @DisplayName("Should publish user login event to RabbitMQ")
        void publishUserLogin_ShouldSendMessageToExchange() {
            UserLoginEvent event = UserLoginEvent.builder()
                    .userId(1L)
                    .email("test@example.com")
                    .loginTime(LocalDateTime.now())
                    .ipAddress("127.0.0.1")
                    .userAgent("TestAgent")
                    .loginMethod("WEB")
                    .build();

            userEventPublisher.publishUserLogin(event);

            verify(rabbitTemplate).convertAndSend(eq(TEST_EXCHANGE), eq(USER_LOGIN_ROUTING_KEY), eq(event));
        }

        @Test
        @DisplayName("Should log service start and end for user login")
        void publishUserLogin_ShouldLogServiceStartAndEnd() {
            UserLoginEvent event = UserLoginEvent.builder()
                    .userId(1L)
                    .build();

            userEventPublisher.publishUserLogin(event);

            verify(loggingUtils).logServiceStart(eq("UserEventPublisher"), eq("publishUserLogin"), any());
            verify(loggingUtils).logServiceEnd(eq("UserEventPublisher"), eq("publishUserLogin"), any());
        }
    }
}
