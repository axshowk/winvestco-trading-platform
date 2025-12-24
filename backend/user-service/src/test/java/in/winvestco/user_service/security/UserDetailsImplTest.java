package in.winvestco.user_service.security;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.user_service.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserDetailsImpl
 * Tests Spring Security UserDetails implementation
 */
@DisplayName("UserDetailsImpl Tests")
class UserDetailsImplTest {

    private User testUser;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD_HASH = "$2a$10$encodedPasswordHash";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
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
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create UserDetailsImpl from User entity")
        void constructor_WithUser_ShouldCreateInstance() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails.getId()).isEqualTo(TEST_USER_ID);
            assertThat(userDetails.getUsername()).isEqualTo(TEST_EMAIL);
            assertThat(userDetails.getPassword()).isEqualTo(TEST_PASSWORD_HASH);
        }

        @Test
        @DisplayName("Should create UserDetailsImpl with all parameters")
        void constructor_WithParameters_ShouldCreateInstance() {
            Collection<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"));

            UserDetailsImpl userDetails = new UserDetailsImpl(
                    TEST_USER_ID,
                    TEST_EMAIL,
                    TEST_PASSWORD_HASH,
                    authorities);

            assertThat(userDetails.getId()).isEqualTo(TEST_USER_ID);
            assertThat(userDetails.getUsername()).isEqualTo(TEST_EMAIL);
            assertThat(userDetails.getPassword()).isEqualTo(TEST_PASSWORD_HASH);
            assertThat(userDetails.getAuthorities()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Build Method Tests")
    class BuildMethodTests {

        @Test
        @DisplayName("Should build UserDetailsImpl from User using static method")
        void build_WithUser_ShouldReturnUserDetailsImpl() {
            UserDetailsImpl userDetails = UserDetailsImpl.build(testUser);

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getId()).isEqualTo(TEST_USER_ID);
            assertThat(userDetails.getUsername()).isEqualTo(TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("Authority Tests")
    class AuthorityTests {

        @Test
        @DisplayName("Should map user roles to granted authorities")
        void getAuthorities_ShouldReturnMappedRoles() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            assertThat(authorities).hasSize(2);
            assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("Should prefix roles with ROLE_")
        void getAuthorities_ShouldPrefixRolesCorrectly() {
            testUser.setRoles(Set.of(Role.VIEWER));
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            assertThat(authorities).hasSize(1);
            assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_VIEWER");
        }
    }

    @Nested
    @DisplayName("Account Status Tests")
    class AccountStatusTests {

        @Test
        @DisplayName("Should return true for isAccountNonExpired")
        void isAccountNonExpired_ShouldReturnTrue() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("Should return true for isAccountNonLocked")
        void isAccountNonLocked_ShouldReturnTrue() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("Should return true for isCredentialsNonExpired")
        void isCredentialsNonExpired_ShouldReturnTrue() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("Should return true for isEnabled")
        void isEnabled_ShouldReturnTrue() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same id")
        void equals_WithSameId_ShouldReturnTrue() {
            UserDetailsImpl userDetails1 = new UserDetailsImpl(testUser);
            UserDetailsImpl userDetails2 = new UserDetailsImpl(testUser);

            assertThat(userDetails1).isEqualTo(userDetails2);
        }

        @Test
        @DisplayName("Should not be equal when different id")
        void equals_WithDifferentId_ShouldReturnFalse() {
            UserDetailsImpl userDetails1 = new UserDetailsImpl(testUser);

            User anotherUser = User.builder()
                    .id(2L)
                    .email("another@example.com")
                    .passwordHash(TEST_PASSWORD_HASH)
                    .roles(Set.of(Role.USER))
                    .status(AccountStatus.ACTIVE)
                    .build();
            UserDetailsImpl userDetails2 = new UserDetailsImpl(anotherUser);

            assertThat(userDetails1).isNotEqualTo(userDetails2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void equals_WithNull_ShouldReturnFalse() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void equals_WithDifferentType_ShouldReturnFalse() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails).isNotEqualTo("string");
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct id")
        void getId_ShouldReturnUserId() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails.getId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should return email as username")
        void getUsername_ShouldReturnEmail() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails.getUsername()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should return password hash")
        void getPassword_ShouldReturnPasswordHash() {
            UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

            assertThat(userDetails.getPassword()).isEqualTo(TEST_PASSWORD_HASH);
        }
    }
}
