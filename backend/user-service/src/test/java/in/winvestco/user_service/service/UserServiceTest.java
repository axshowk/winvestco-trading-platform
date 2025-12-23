package in.winvestco.user_service.service;

import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.common.util.LoggingUtils;
import in.winvestco.user_service.dto.UserResponse;
import in.winvestco.user_service.exception.UserNotFoundException;
import in.winvestco.user_service.model.User;
import in.winvestco.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 * Uses Mockito to mock dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserEventPublisher userEventPublisher;

    @Mock
    private LoggingUtils loggingUtils;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ENCODED_PASSWORD = "$2a$10$encodedPasswordHashThatIsLongEnoughToPass";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .email(TEST_EMAIL)
                .firstName("John")
                .lastName("Doe")
                .passwordHash(TEST_ENCODED_PASSWORD)
                .phoneNumber("1234567890")
                .roles(Set.of(Role.USER))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn("test@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Find Operations Tests")
    class FindOperationsTests {

        @Test
        @DisplayName("Should return UserResponse when findById finds existing user")
        void findById_WhenUserExists_ShouldReturnUserResponse() {
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

            UserResponse result = userService.findById(TEST_USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
            verify(userRepository).findById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should return null when findById doesn't find user")
        void findById_WhenUserNotExists_ShouldReturnNull() {
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            UserResponse result = userService.findById(TEST_USER_ID);

            assertThat(result).isNull();
            verify(userRepository).findById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should return UserResponse when findByEmail finds existing user")
        void findByEmail_WhenUserExists_ShouldReturnUserResponse() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            UserResponse result = userService.findByEmail(TEST_EMAIL);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEST_USER_ID);
            verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should return true when email exists")
        void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            boolean exists = userService.existsByEmail(TEST_EMAIL);

            assertThat(exists).isTrue();
            verify(userRepository).existsByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should return false when email doesn't exist")
        void existsByEmail_WhenEmailNotExists_ShouldReturnFalse() {
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

            boolean exists = userService.existsByEmail(TEST_EMAIL);

            assertThat(exists).isFalse();
            verify(userRepository).existsByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should return all users as UserResponse list")
        void findAll_ShouldReturnAllUserResponses() {
            User user2 = User.builder()
                    .id(2L)
                    .email("user2@example.com")
                    .firstName("Jane")
                    .lastName("Doe")
                    .passwordHash(TEST_ENCODED_PASSWORD)
                    .roles(Set.of(Role.USER))
                    .status(AccountStatus.ACTIVE)
                    .build();
            when(userRepository.findAll()).thenReturn(List.of(testUser, user2));

            List<UserResponse> result = userService.findAll();

            assertThat(result).hasSize(2);
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should return users by status")
        void findAllByStatus_ShouldReturnFilteredUsers() {
            when(userRepository.findAllByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(testUser));

            List<UserResponse> result = userService.findAllByStatus(AccountStatus.ACTIVE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should return users by role")
        void findAllByRole_ShouldReturnFilteredUsers() {
            when(userRepository.findAllByRolesContaining(Role.USER)).thenReturn(List.of(testUser));

            List<UserResponse> result = userService.findAllByRole(Role.USER);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo(TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("Register Operations Tests")
    class RegisterOperationsTests {

        @Test
        @DisplayName("Should register new user with default roles")
        void register_ShouldCreateUserWithDefaultRoles() {
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                savedUser.setId(TEST_USER_ID);
                return savedUser;
            });

            User result = userService.register(TEST_EMAIL, "John", "Doe", TEST_PASSWORD, "1234567890");

            assertThat(result).isNotNull();
            assertThat(result.getRoles()).contains(Role.USER, Role.VIEWER);
            verify(userEventPublisher).publishUserCreated(any());
        }

        @Test
        @DisplayName("Should throw exception when registering with existing email")
        void register_WhenEmailExists_ShouldThrowException() {
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> userService.register(TEST_EMAIL, "John", "Doe", TEST_PASSWORD, "1234567890"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already in use");
        }
    }

    @Nested
    @DisplayName("Update Operations Tests")
    class UpdateOperationsTests {

        @Test
        @DisplayName("Should update user fields correctly")
        void update_ShouldUpdateUserFields() {
            when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            testUser.setFirstName("UpdatedName");
            User result = userService.update(testUser);

            assertThat(result).isNotNull();
            verify(userRepository).save(testUser);
            verify(userEventPublisher).publishUserUpdated(any());
        }

        @Test
        @DisplayName("Should throw exception when update user has no ID")
        void update_WhenNoId_ShouldThrowException() {
            testUser.setId(null);
            assertThatThrownBy(() -> userService.update(testUser))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User id is required");
        }

        @Test
        @DisplayName("Should throw exception when update user not found")
        void update_WhenUserNotFound_ShouldThrowException() {
            when(userRepository.existsById(TEST_USER_ID)).thenReturn(false);
            assertThatThrownBy(() -> userService.update(testUser))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should update account status and publish event")
        void updateStatus_ShouldUpdateStatusAndPublishEvent() {
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserResponse result = userService.updateStatus(TEST_USER_ID, AccountStatus.SUSPENDED);

            assertThat(result).isNotNull();
            verify(userRepository).save(any(User.class));
            verify(userEventPublisher).publishUserStatusChanged(any());
        }

        @Test
        @DisplayName("Should update roles and publish event")
        void updateRoles_ShouldUpdateRolesAndPublishEvent() {
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            Set<Role> newRoles = Set.of(Role.USER, Role.ADMIN);
            UserResponse result = userService.updateRoles(TEST_USER_ID, newRoles);

            assertThat(result).isNotNull();
            verify(userRepository).save(any(User.class));
            verify(userEventPublisher).publishUserRoleChanged(any());
        }
    }

    @Nested
    @DisplayName("Password Operations Tests")
    class PasswordOperationsTests {

        @Test
        @DisplayName("Should change password with correct old password")
        void changePassword_WithCorrectOldPassword_ShouldSucceed() {
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(TEST_PASSWORD, TEST_ENCODED_PASSWORD)).thenReturn(true);
            when(passwordEncoder.encode("newPassword")).thenReturn("$2a$10$newEncodedPasswordHashLong");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.changePassword(TEST_USER_ID, TEST_PASSWORD, "newPassword");

            verify(passwordEncoder).encode("newPassword");
            verify(userRepository).save(any(User.class));
            verify(userEventPublisher).publishUserPasswordChanged(any());
        }

        @Test
        @DisplayName("Should throw exception with incorrect old password")
        void changePassword_WithIncorrectOldPassword_ShouldThrowException() {
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(TEST_USER_ID, "wrongPassword", "newPassword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Old password is incorrect");
        }
    }

    @Nested
    @DisplayName("Delete Operations Tests")
    class DeleteOperationsTests {

        @Test
        @DisplayName("Should delete existing user")
        void deleteById_WhenUserExists_ShouldDelete() {
            when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
            doNothing().when(userRepository).deleteById(TEST_USER_ID);

            userService.deleteById(TEST_USER_ID);

            verify(userRepository).deleteById(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent user")
        void deleteById_WhenUserNotExists_ShouldThrowException() {
            when(userRepository.existsById(TEST_USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteById(TEST_USER_ID))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Login and Context Operations Tests")
    class LoginAndContextOperationsTests {

        @Test
        @DisplayName("Should mark last login and publish event")
        void markLastLogin_ShouldUpdateTimestampAndPublishEvent() {
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.markLastLogin(TEST_USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getLastLoginAt()).isNotNull();
            verify(userEventPublisher).publishUserLogin(any());
        }

        @Test
        @DisplayName("Should return current user from security context")
        void getCurrentUser_ShouldReturnUserFromContext() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            User result = userService.getCurrentUser();

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }
    }
}
