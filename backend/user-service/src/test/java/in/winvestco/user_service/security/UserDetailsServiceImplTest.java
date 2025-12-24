package in.winvestco.user_service.security;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.common.util.LoggingUtils;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserDetailsServiceImpl
 * Tests user loading for Spring Security authentication
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoggingUtils loggingUtils;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD_HASH = "$2a$10$encodedPasswordHashThatIsLongEnoughToPass123456789012";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .firstName("John")
                .lastName("Doe")
                .passwordHash(TEST_PASSWORD_HASH)
                .phoneNumber("1234567890")
                .roles(Set.of(Role.USER, Role.ADMIN))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("LoadUserByUsername Success Tests")
    class LoadUserByUsernameSuccessTests {

        @Test
        @DisplayName("Should return UserDetails when user exists and is active")
        void loadUserByUsername_WithActiveUser_ShouldReturnUserDetails() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            UserDetails result = userDetailsService.loadUserByUsername(TEST_EMAIL);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(TEST_EMAIL);
            assertThat(result.getPassword()).isEqualTo(TEST_PASSWORD_HASH);
            verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should include all user roles as authorities")
        void loadUserByUsername_ShouldMapRolesToAuthorities() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            UserDetails result = userDetailsService.loadUserByUsername(TEST_EMAIL);

            assertThat(result.getAuthorities()).hasSize(2);
            assertThat(result.getAuthorities())
                    .extracting(auth -> auth.getAuthority())
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("Should return UserDetailsImpl type")
        void loadUserByUsername_ShouldReturnUserDetailsImplType() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            UserDetails result = userDetailsService.loadUserByUsername(TEST_EMAIL);

            assertThat(result).isInstanceOf(UserDetailsImpl.class);
        }
    }

    @Nested
    @DisplayName("LoadUserByUsername Failure Tests")
    class LoadUserByUsernameFailureTests {

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found")
        void loadUserByUsername_WhenUserNotFound_ShouldThrowException() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when email is null")
        void loadUserByUsername_WhenEmailNull_ShouldThrowException() {
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Email is null or empty");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when email is empty")
        void loadUserByUsername_WhenEmailEmpty_ShouldThrowException() {
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Email is null or empty");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when email is whitespace")
        void loadUserByUsername_WhenEmailWhitespace_ShouldThrowException() {
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("   "))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Email is null or empty");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user is not active")
        void loadUserByUsername_WhenUserNotActive_ShouldThrowException() {
            testUser.setStatus(AccountStatus.SUSPENDED);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(TEST_EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User is not active");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user is locked")
        void loadUserByUsername_WhenUserLocked_ShouldThrowException() {
            testUser.setStatus(AccountStatus.LOCKED);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(TEST_EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User is not active");
        }
    }

    @Nested
    @DisplayName("Logging Tests")
    class LoggingTests {

        @Test
        @DisplayName("Should log service start when loading user")
        void loadUserByUsername_ShouldLogServiceStart() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            userDetailsService.loadUserByUsername(TEST_EMAIL);

            verify(loggingUtils).logServiceStart(eq("UserDetailsServiceImpl"), eq("loadUserByUsername"), any());
        }

        @Test
        @DisplayName("Should log service end when user loaded successfully")
        void loadUserByUsername_ShouldLogServiceEnd() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            userDetailsService.loadUserByUsername(TEST_EMAIL);

            verify(loggingUtils).logServiceEnd(eq("UserDetailsServiceImpl"), eq("loadUserByUsername"), any());
        }

        @Test
        @DisplayName("Should log debug messages during user loading")
        void loadUserByUsername_ShouldLogDebugMessages() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            userDetailsService.loadUserByUsername(TEST_EMAIL);

            verify(loggingUtils, atLeastOnce()).logDebug(anyString(), any());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle user with single role")
        void loadUserByUsername_WithSingleRole_ShouldReturnUserDetails() {
            testUser.setRoles(Set.of(Role.VIEWER));
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            UserDetails result = userDetailsService.loadUserByUsername(TEST_EMAIL);

            assertThat(result.getAuthorities()).hasSize(1);
            assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_VIEWER");
        }

        @Test
        @DisplayName("Should handle user with no roles")
        void loadUserByUsername_WithNoRoles_ShouldReturnUserDetails() {
            testUser.setRoles(Set.of());
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            UserDetails result = userDetailsService.loadUserByUsername(TEST_EMAIL);

            assertThat(result.getAuthorities()).isEmpty();
        }
    }
}
