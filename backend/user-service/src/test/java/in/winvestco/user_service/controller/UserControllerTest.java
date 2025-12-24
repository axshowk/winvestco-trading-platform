package in.winvestco.user_service.controller;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.common.util.LoggingUtils;
import in.winvestco.user_service.dto.RegisterRequest;
import in.winvestco.user_service.dto.UserResponse;
import in.winvestco.user_service.model.User;
import in.winvestco.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController
 * Uses Mockito to test controller logic in isolation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private LoggingUtils loggingUtils;

    @InjectMocks
    private UserController userController;

    private UserResponse testUserResponse;
    private User testUser;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUserResponse = new UserResponse();
        testUserResponse.setId(TEST_USER_ID);
        testUserResponse.setEmail(TEST_EMAIL);
        testUserResponse.setFirstName("John");
        testUserResponse.setLastName("Doe");
        testUserResponse.setPhoneNumber("1234567890");
        testUserResponse.setStatus(AccountStatus.ACTIVE);
        testUserResponse.setRoles(Set.of(Role.USER));
        testUserResponse.setCreatedAt(Instant.now());

        testUser = User.builder()
                .id(TEST_USER_ID)
                .email(TEST_EMAIL)
                .firstName("John")
                .lastName("Doe")
                .passwordHash("$2a$10$encodedPassword")
                .phoneNumber("1234567890")
                .status(AccountStatus.ACTIVE)
                .roles(Set.of(Role.USER))
                .build();
    }

    @Nested
    @DisplayName("findAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all users when no filters provided")
        void findAll_WithNoFilters_ShouldReturnAllUsers() {
            when(userService.findAll()).thenReturn(List.of(testUserResponse));

            List<UserResponse> result = userController.findAll(null, null, null, null, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo(TEST_EMAIL);
            verify(userService).findAll();
        }

        @Test
        @DisplayName("Should filter by email when provided")
        void findAll_WithEmailFilter_ShouldReturnFilteredUsers() {
            when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUserResponse);

            List<UserResponse> result = userController.findAll(null, null, null, null, TEST_EMAIL, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo(TEST_EMAIL);
            verify(userService).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should return empty list when email not found")
        void findAll_WithEmailNotFound_ShouldReturnEmptyList() {
            when(userService.findByEmail("nonexistent@example.com")).thenReturn(null);

            List<UserResponse> result = userController.findAll(null, null, null, null, "nonexistent@example.com", null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should filter by status when provided")
        void findAll_WithStatusFilter_ShouldReturnFilteredUsers() {
            when(userService.findAllByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(testUserResponse));

            List<UserResponse> result = userController.findAll(AccountStatus.ACTIVE, null, null, null, null, null);

            assertThat(result).hasSize(1);
            verify(userService).findAllByStatus(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should filter by role when provided")
        void findAll_WithRoleFilter_ShouldReturnFilteredUsers() {
            when(userService.findAllByRole(Role.USER)).thenReturn(List.of(testUserResponse));

            List<UserResponse> result = userController.findAll(null, Role.USER, null, null, null, null);

            assertThat(result).hasSize(1);
            verify(userService).findAllByRole(Role.USER);
        }

        @Test
        @DisplayName("Should filter by firstName when provided")
        void findAll_WithFirstNameFilter_ShouldReturnFilteredUsers() {
            when(userService.findByFirstName("John")).thenReturn(List.of(testUserResponse));

            List<UserResponse> result = userController.findAll(null, null, "John", null, null, null);

            assertThat(result).hasSize(1);
            verify(userService).findByFirstName("John");
        }

        @Test
        @DisplayName("Should filter by lastName when provided")
        void findAll_WithLastNameFilter_ShouldReturnFilteredUsers() {
            when(userService.findByLastName("Doe")).thenReturn(List.of(testUserResponse));

            List<UserResponse> result = userController.findAll(null, null, null, "Doe", null, null);

            assertThat(result).hasSize(1);
            verify(userService).findByLastName("Doe");
        }

        @Test
        @DisplayName("Should filter by phoneNumber when provided")
        void findAll_WithPhoneNumberFilter_ShouldReturnFilteredUsers() {
            when(userService.findByPhoneNumber("1234567890")).thenReturn(List.of(testUserResponse));

            List<UserResponse> result = userController.findAll(null, null, null, null, null, "1234567890");

            assertThat(result).hasSize(1);
            verify(userService).findByPhoneNumber("1234567890");
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return user when found")
        void findById_WhenUserExists_ShouldReturnUser() {
            when(userService.findById(TEST_USER_ID)).thenReturn(testUserResponse);

            ResponseEntity<UserResponse> result = userController.findById(TEST_USER_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getId()).isEqualTo(TEST_USER_ID);
            verify(userService).findById(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void register_WithValidRequest_ShouldReturnCreated() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("new@example.com");
            request.setPassword("password123");
            request.setFirstName("New");
            request.setLastName("User");
            request.setPhoneNumber("9876543210");

            User savedUser = User.builder()
                    .id(2L)
                    .email("new@example.com")
                    .firstName("New")
                    .lastName("User")
                    .passwordHash("$2a$10$encoded")
                    .phoneNumber("9876543210")
                    .status(AccountStatus.ACTIVE)
                    .roles(Set.of(Role.USER))
                    .build();

            when(userService.register(any(), any(), any(), any(), any())).thenReturn(savedUser);

            ResponseEntity<UserResponse> result = userController.register(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getEmail()).isEqualTo("new@example.com");
        }
    }

    @Nested
    @DisplayName("update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update user successfully")
        void update_WithValidUser_ShouldReturnUpdatedUser() {
            when(userService.update(any(User.class))).thenReturn(testUser);

            UserResponse result = userController.update(TEST_USER_ID, testUser);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
            verify(userService).update(testUser);
        }

        @Test
        @DisplayName("Should set user id from path variable")
        void update_ShouldSetUserIdFromPathVariable() {
            when(userService.update(any(User.class))).thenReturn(testUser);

            userController.update(TEST_USER_ID, testUser);

            assertThat(testUser.getId()).isEqualTo(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete user successfully")
        void delete_ShouldCallServiceDelete() {
            doNothing().when(userService).deleteById(TEST_USER_ID);

            userController.delete(TEST_USER_ID);

            verify(userService).deleteById(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("updateStatus Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update user status successfully")
        void updateStatus_ShouldReturnUpdatedUser() {
            testUserResponse.setStatus(AccountStatus.SUSPENDED);
            when(userService.updateStatus(eq(TEST_USER_ID), eq(AccountStatus.SUSPENDED)))
                    .thenReturn(testUserResponse);

            UserResponse result = userController.updateStatus(TEST_USER_ID, AccountStatus.SUSPENDED);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
            verify(userService).updateStatus(TEST_USER_ID, AccountStatus.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("updateRoles Tests")
    class UpdateRolesTests {

        @Test
        @DisplayName("Should update user roles successfully")
        void updateRoles_ShouldReturnUpdatedUser() {
            Set<Role> newRoles = Set.of(Role.USER, Role.ADMIN);
            testUserResponse.setRoles(newRoles);
            when(userService.updateRoles(eq(TEST_USER_ID), eq(newRoles))).thenReturn(testUserResponse);

            UserResponse result = userController.updateRoles(TEST_USER_ID, newRoles);

            assertThat(result).isNotNull();
            assertThat(result.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
            verify(userService).updateRoles(TEST_USER_ID, newRoles);
        }
    }
}
