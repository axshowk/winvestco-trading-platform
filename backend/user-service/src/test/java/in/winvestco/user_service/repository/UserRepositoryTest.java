package in.winvestco.user_service.repository;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.user_service.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests using @DataJpaTest
 * Uses H2 in-memory database for testing
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private static final String TEST_ENCODED_PASSWORD = "$2a$10$encodedPasswordHashThatIsLongEnoughToPass123456";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash(TEST_ENCODED_PASSWORD)
                .phoneNumber("1234567890")
                .roles(Set.of(Role.USER))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should save and find user by email")
    void findByEmail_WhenUserExists_ShouldReturnUser() {
        entityManager.persistAndFlush(testUser);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void findByEmail_WhenEmailNotExists_ShouldReturnEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return true when email exists")
    void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
        entityManager.persistAndFlush(testUser);

        boolean exists = userRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email doesn't exist")
    void existsByEmail_WhenEmailNotExists_ShouldReturnFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should find users by status")
    void findAllByStatus_ShouldReturnUsersWithMatchingStatus() {
        User activeUser = User.builder()
                .email("active@example.com")
                .firstName("Active")
                .lastName("User")
                .passwordHash(TEST_ENCODED_PASSWORD)
                .roles(Set.of(Role.USER))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User suspendedUser = User.builder()
                .email("suspended@example.com")
                .firstName("Suspended")
                .lastName("User")
                .passwordHash(TEST_ENCODED_PASSWORD)
                .roles(Set.of(Role.USER))
                .status(AccountStatus.SUSPENDED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        entityManager.persistAndFlush(activeUser);
        entityManager.persistAndFlush(suspendedUser);

        List<User> activeUsers = userRepository.findAllByStatus(AccountStatus.ACTIVE);

        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getEmail()).isEqualTo("active@example.com");
    }

    @Test
    @DisplayName("Should find users by role")
    void findAllByRolesContaining_ShouldReturnUsersWithMatchingRole() {
        User adminUser = User.builder()
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .passwordHash(TEST_ENCODED_PASSWORD)
                .roles(Set.of(Role.ADMIN, Role.USER))
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(adminUser);

        List<User> adminUsers = userRepository.findAllByRolesContaining(Role.ADMIN);

        assertThat(adminUsers).hasSize(1);
        assertThat(adminUsers.get(0).getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("Should find users by first name")
    void findByFirstName_ShouldReturnMatchingUsers() {
        entityManager.persistAndFlush(testUser);

        List<User> users = userRepository.findByFirstName("John");

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("test@example.com");
    }
}
