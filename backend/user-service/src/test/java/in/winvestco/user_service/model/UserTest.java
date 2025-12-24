package in.winvestco.user_service.model;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for User entity
 * Tests entity construction, validation, and lifecycle hooks
 */
@DisplayName("User Entity Tests")
class UserTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("$2a$10$encodedPasswordHashThatIsLongEnoughToPass")
                .phoneNumber("1234567890")
                .roles(Set.of(Role.USER))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create user with builder")
        void builder_ShouldCreateUser() {
            User user = User.builder()
                    .email("new@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .passwordHash("$2a$10$hashedPasswordThatIsLongEnoughToPassValidation")
                    .build();

            assertThat(user).isNotNull();
            assertThat(user.getEmail()).isEqualTo("new@example.com");
            assertThat(user.getFirstName()).isEqualTo("Jane");
        }

        @Test
        @DisplayName("Should set default status to ACTIVE")
        void builder_ShouldSetDefaultStatus() {
            User user = User.builder()
                    .email("test@example.com")
                    .passwordHash("$2a$10$hashedPasswordThatIsLongEnoughToPassValidation")
                    .build();

            assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should set default roles to empty set")
        void builder_ShouldSetDefaultRoles() {
            User user = User.builder()
                    .email("test@example.com")
                    .passwordHash("$2a$10$hashedPasswordThatIsLongEnoughToPassValidation")
                    .build();

            assertThat(user.getRoles()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should get and set id")
        void getSetId_ShouldWork() {
            testUser.setId(2L);
            assertThat(testUser.getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should get and set email")
        void getSetEmail_ShouldWork() {
            testUser.setEmail("updated@example.com");
            assertThat(testUser.getEmail()).isEqualTo("updated@example.com");
        }

        @Test
        @DisplayName("Should get and set firstName")
        void getSetFirstName_ShouldWork() {
            testUser.setFirstName("Jane");
            assertThat(testUser.getFirstName()).isEqualTo("Jane");
        }

        @Test
        @DisplayName("Should get and set lastName")
        void getSetLastName_ShouldWork() {
            testUser.setLastName("Smith");
            assertThat(testUser.getLastName()).isEqualTo("Smith");
        }

        @Test
        @DisplayName("Should get and set phoneNumber")
        void getSetPhoneNumber_ShouldWork() {
            testUser.setPhoneNumber("9876543210");
            assertThat(testUser.getPhoneNumber()).isEqualTo("9876543210");
        }

        @Test
        @DisplayName("Should get and set status")
        void getSetStatus_ShouldWork() {
            testUser.setStatus(AccountStatus.SUSPENDED);
            assertThat(testUser.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        }

        @Test
        @DisplayName("Should get and set roles")
        void getSetRoles_ShouldWork() {
            testUser.setRoles(Set.of(Role.ADMIN, Role.USER));
            assertThat(testUser.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.USER);
        }

        @Test
        @DisplayName("Should get and set lastLoginAt")
        void getSetLastLoginAt_ShouldWork() {
            Instant now = Instant.now();
            testUser.setLastLoginAt(now);
            assertThat(testUser.getLastLoginAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should get and set clientId")
        void getSetClientId_ShouldWork() {
            testUser.setClientId("WIN-custom123");
            assertThat(testUser.getClientId()).isEqualTo("WIN-custom123");
        }
    }

    @Nested
    @DisplayName("PrePersist Tests")
    class PrePersistTests {

        @Test
        @DisplayName("Should generate clientId on persist if null")
        void onCreate_WhenClientIdNull_ShouldGenerateClientId() {
            User user = User.builder()
                    .email("test@example.com")
                    .passwordHash("$2a$10$hashedPasswordThatIsLongEnoughToPassValidation")
                    .build();

            assertThat(user.getClientId()).isNull();

            // Simulate @PrePersist
            user.onCreate();

            assertThat(user.getClientId()).isNotNull();
            assertThat(user.getClientId()).startsWith("WIN-");
        }

        @Test
        @DisplayName("Should not override existing clientId")
        void onCreate_WhenClientIdExists_ShouldNotOverride() {
            User user = User.builder()
                    .email("test@example.com")
                    .passwordHash("$2a$10$hashedPasswordThatIsLongEnoughToPassValidation")
                    .clientId("WIN-existing")
                    .build();

            user.onCreate();

            assertThat(user.getClientId()).isEqualTo("WIN-existing");
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same id")
        void equals_WithSameId_ShouldReturnTrue() {
            User user1 = User.builder().id(1L).email("a@test.com")
                    .passwordHash("$2a$10$hash1ThatIsLongEnoughForValidation").build();
            User user2 = User.builder().id(1L).email("b@test.com")
                    .passwordHash("$2a$10$hash2ThatIsLongEnoughForValidation").build();

            assertThat(user1).isEqualTo(user2);
        }

        @Test
        @DisplayName("Should not be equal when different id")
        void equals_WithDifferentId_ShouldReturnFalse() {
            User user1 = User.builder().id(1L).email("a@test.com")
                    .passwordHash("$2a$10$hash1ThatIsLongEnoughForValidation").build();
            User user2 = User.builder().id(2L).email("a@test.com")
                    .passwordHash("$2a$10$hash1ThatIsLongEnoughForValidation").build();

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("Should have consistent hashCode based on id")
        void hashCode_WithSameId_ShouldBeEqual() {
            User user1 = User.builder().id(1L).email("a@test.com")
                    .passwordHash("$2a$10$hash1ThatIsLongEnoughForValidation").build();
            User user2 = User.builder().id(1L).email("b@test.com")
                    .passwordHash("$2a$10$hash2ThatIsLongEnoughForValidation").build();

            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should not include password hash in toString")
        void toString_ShouldNotIncludePasswordHash() {
            String str = testUser.toString();

            assertThat(str).doesNotContain("$2a$10$");
        }

        @Test
        @DisplayName("Should include email in toString")
        void toString_ShouldIncludeEmail() {
            String str = testUser.toString();

            assertThat(str).contains("test@example.com");
        }
    }

    @Nested
    @DisplayName("No-Args Constructor Tests")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("Should create user with no-args constructor")
        void noArgsConstructor_ShouldCreateUser() {
            User user = new User();

            assertThat(user).isNotNull();
            assertThat(user.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("All-Args Constructor Tests")
    class AllArgsConstructorTests {

        @Test
        @DisplayName("Should create user with all-args constructor")
        void allArgsConstructor_ShouldCreateUser() {
            Instant now = Instant.now();
            User user = new User(
                    1L, "test@example.com", "$2a$10$hashedPasswordThatIsLongEnoughForValidation",
                    "John", "Doe", "WIN-123", "1234567890",
                    AccountStatus.ACTIVE, Set.of(Role.USER), now, now, now);

            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getFirstName()).isEqualTo("John");
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("Should implement Serializable")
        void class_ShouldImplementSerializable() {
            assertThat(testUser).isInstanceOf(java.io.Serializable.class);
        }
    }
}
