package in.winvestco.user_service.dto;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.user_service.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserResponse DTO
 * Tests conversion from User entity to DTO
 */
@DisplayName("UserResponse Tests")
class UserResponseTest {

    private User testUser;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_CLIENT_ID = "WIN-abc123";
    private static final String TEST_EMAIL = "test@example.com";
    private static final Instant TEST_CREATED_AT = Instant.now();
    private static final Instant TEST_LAST_LOGIN_AT = Instant.now();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .clientId(TEST_CLIENT_ID)
                .email(TEST_EMAIL)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .passwordHash("$2a$10$encodedPassword")
                .roles(Set.of(Role.USER, Role.ADMIN))
                .status(AccountStatus.ACTIVE)
                .createdAt(TEST_CREATED_AT)
                .lastLoginAt(TEST_LAST_LOGIN_AT)
                .build();
    }

    @Nested
    @DisplayName("fromUser Static Method Tests")
    class FromUserTests {

        @Test
        @DisplayName("Should convert User to UserResponse with all fields")
        void fromUser_ShouldMapAllFields() {
            UserResponse response = UserResponse.fromUser(testUser);

            assertThat(response.getId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getClientId()).isEqualTo(TEST_CLIENT_ID);
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getFirstName()).isEqualTo("John");
            assertThat(response.getLastName()).isEqualTo("Doe");
            assertThat(response.getPhoneNumber()).isEqualTo("1234567890");
            assertThat(response.getCreatedAt()).isEqualTo(TEST_CREATED_AT);
            assertThat(response.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
            assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(response.getLastLoginAt()).isEqualTo(TEST_LAST_LOGIN_AT);
        }

        @Test
        @DisplayName("Should not include password hash in response")
        void fromUser_ShouldNotExposePasswordHash() {
            UserResponse response = UserResponse.fromUser(testUser);

            // UserResponse doesn't have a password field - this is by design
            assertThat(response.toString()).doesNotContain("password");
            assertThat(response.toString()).doesNotContain("$2a$10$");
        }

        @Test
        @DisplayName("Should handle null optional fields")
        void fromUser_WithNullOptionalFields_ShouldReturnResponseWithNulls() {
            testUser.setPhoneNumber(null);
            testUser.setLastLoginAt(null);
            testUser.setClientId(null);

            UserResponse response = UserResponse.fromUser(testUser);

            assertThat(response.getPhoneNumber()).isNull();
            assertThat(response.getLastLoginAt()).isNull();
            assertThat(response.getClientId()).isNull();
        }

        @Test
        @DisplayName("Should handle empty roles set")
        void fromUser_WithEmptyRoles_ShouldReturnEmptyRolesSet() {
            testUser.setRoles(Set.of());

            UserResponse response = UserResponse.fromUser(testUser);

            assertThat(response.getRoles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create UserResponse with no-arg constructor")
        void noArgConstructor_ShouldCreateEmptyResponse() {
            UserResponse response = new UserResponse();

            assertThat(response.getId()).isNull();
            assertThat(response.getEmail()).isNull();
        }

        @Test
        @DisplayName("Should create UserResponse with all-arg constructor")
        void allArgConstructor_ShouldCreateFullResponse() {
            Set<Role> roles = Set.of(Role.USER);

            UserResponse response = new UserResponse(
                    TEST_USER_ID,
                    TEST_CLIENT_ID,
                    TEST_EMAIL,
                    "John",
                    "Doe",
                    "1234567890",
                    TEST_CREATED_AT,
                    roles,
                    AccountStatus.ACTIVE,
                    TEST_LAST_LOGIN_AT);

            assertThat(response.getId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getRoles()).containsExactly(Role.USER);
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("Should set all fields using setters")
        void setters_ShouldUpdateFields() {
            UserResponse response = new UserResponse();

            response.setId(TEST_USER_ID);
            response.setEmail(TEST_EMAIL);
            response.setFirstName("Jane");
            response.setLastName("Smith");
            response.setPhoneNumber("9876543210");
            response.setStatus(AccountStatus.SUSPENDED);
            response.setRoles(Set.of(Role.ADMIN));

            assertThat(response.getId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getFirstName()).isEqualTo("Jane");
            assertThat(response.getLastName()).isEqualTo("Smith");
            assertThat(response.getPhoneNumber()).isEqualTo("9876543210");
            assertThat(response.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
            assertThat(response.getRoles()).containsExactly(Role.ADMIN);
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void equals_WithSameFields_ShouldReturnTrue() {
            UserResponse response1 = UserResponse.fromUser(testUser);
            UserResponse response2 = UserResponse.fromUser(testUser);

            assertThat(response1).isEqualTo(response2);
        }

        @Test
        @DisplayName("Should have consistent hashCode")
        void hashCode_WithSameFields_ShouldBeEqual() {
            UserResponse response1 = UserResponse.fromUser(testUser);
            UserResponse response2 = UserResponse.fromUser(testUser);

            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("Should implement Serializable")
        void class_ShouldImplementSerializable() {
            UserResponse response = new UserResponse();

            assertThat(response).isInstanceOf(java.io.Serializable.class);
        }
    }

    @Nested
    @DisplayName("toString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return readable string representation")
        void toString_ShouldReturnReadableString() {
            UserResponse response = UserResponse.fromUser(testUser);

            String str = response.toString();

            assertThat(str).contains("UserResponse");
            assertThat(str).contains(TEST_EMAIL);
        }
    }
}
