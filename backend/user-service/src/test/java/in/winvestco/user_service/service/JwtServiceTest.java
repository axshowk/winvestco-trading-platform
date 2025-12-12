package in.winvestco.user_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtService
 * Tests JWT token generation, extraction, and validation
 */
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUserDetails;
    private static final String TEST_SECRET = "test-secret-key-for-unit-testing-minimum-256-bits-required-here";
    private static final Long TEST_EXPIRATION = 86400000L; // 24 hours
    private static final String TEST_EMAIL = "test@example.com";
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", TEST_EXPIRATION);

        testUserDetails = User.builder()
                .username(TEST_EMAIL)
                .password("password")
                .authorities(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN"))
                .build();
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate a valid JWT token")
        void generateToken_ShouldReturnValidToken() {
            String token = jwtService.generateToken(testUserDetails, TEST_EMAIL, TEST_USER_ID);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("Should include email in token claims")
        void generateToken_ShouldIncludeEmailInClaims() {
            String token = jwtService.generateToken(testUserDetails, TEST_EMAIL, TEST_USER_ID);

            String extractedEmail = jwtService.extractEmail(token);
            assertThat(extractedEmail).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should include userId in token claims")
        void generateToken_ShouldIncludeUserIdInClaims() {
            String token = jwtService.generateToken(testUserDetails, TEST_EMAIL, TEST_USER_ID);

            Long extractedUserId = jwtService.extractUserId(token);
            assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should include roles in token claims")
        void generateToken_ShouldIncludeRolesInClaims() {
            String token = jwtService.generateToken(testUserDetails, TEST_EMAIL, TEST_USER_ID);

            List<String> roles = jwtService.extractRoles(token);
            assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("Token Extraction Tests")
    class TokenExtractionTests {

        private String validToken;

        @BeforeEach
        void setUp() {
            validToken = jwtService.generateToken(testUserDetails, TEST_EMAIL, TEST_USER_ID);
        }

        @Test
        @DisplayName("Should extract username from token")
        void extractUsername_ShouldReturnCorrectUsername() {
            String username = jwtService.extractUsername(validToken);
            assertThat(username).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should extract email from token")
        void extractEmail_ShouldReturnCorrectEmail() {
            String email = jwtService.extractEmail(validToken);
            assertThat(email).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should extract userId from token")
        void extractUserId_ShouldReturnCorrectUserId() {
            Long userId = jwtService.extractUserId(validToken);
            assertThat(userId).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should extract roles from token")
        void extractRoles_ShouldReturnCorrectRoles() {
            List<String> roles = jwtService.extractRoles(validToken);
            assertThat(roles).hasSize(2);
            assertThat(roles).contains("ROLE_USER", "ROLE_ADMIN");
        }

        @Test
        @DisplayName("Should extract expiration date from token")
        void extractExpiration_ShouldReturnFutureDate() {
            Date expiration = jwtService.extractExpiration(validToken);
            assertThat(expiration).isAfter(new Date());
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should return true for valid token with matching user")
        void validateToken_WithValidTokenAndMatchingUser_ShouldReturnTrue() {
            String token = jwtService.generateToken(testUserDetails, TEST_EMAIL, TEST_USER_ID);

            Boolean isValid = jwtService.validateToken(token, testUserDetails);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should return false for token with mismatched username")
        void validateToken_WithMismatchedUsername_ShouldReturnFalse() {
            String token = jwtService.generateToken(testUserDetails, TEST_EMAIL, TEST_USER_ID);

            UserDetails differentUser = User.builder()
                    .username("different@example.com")
                    .password("password")
                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                    .build();

            Boolean isValid = jwtService.validateToken(token, differentUser);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return true for non-expired token")
        void validateToken_WithNonExpiredToken_ShouldReturnTrue() {
            String token = jwtService.generateToken(testUserDetails, TEST_EMAIL, TEST_USER_ID);

            Boolean isValid = jwtService.validateToken(token);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void validateToken_WithMalformedToken_ShouldReturnFalse() {
            String malformedToken = "invalid.token.here";

            Boolean isValid = jwtService.validateToken(malformedToken);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void validateToken_WithExpiredToken_ShouldReturnFalse() {
            // Create a JwtService with very short expiration
            JwtService shortExpirationService = new JwtService();
            ReflectionTestUtils.setField(shortExpirationService, "secret", TEST_SECRET);
            ReflectionTestUtils.setField(shortExpirationService, "expiration", 1L); // 1ms expiration

            String token = shortExpirationService.generateToken(testUserDetails, TEST_EMAIL, TEST_USER_ID);

            // Wait for token to expire
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Boolean isValid = shortExpirationService.validateToken(token);
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should throw exception for null token")
        void extractUsername_WithNullToken_ShouldThrowException() {
            assertThatThrownBy(() -> jwtService.extractUsername(null))
                    .isInstanceOf(Exception.class);
        }
    }
}
