package in.winvestco.user_service.controller;

import in.winvestco.common.enums.AccountStatus;
import in.winvestco.common.enums.Role;
import in.winvestco.common.util.LoggingUtils;
import in.winvestco.user_service.dto.LoginRequest;
import in.winvestco.user_service.dto.UserResponse;
import in.winvestco.user_service.service.JwtService;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Collections;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController
 * Uses Mockito to test controller logic in isolation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private UserService userService;

        @Mock
        private JwtService jwtService;

        @Mock
        private LoggingUtils loggingUtils;

        @InjectMocks
        private AuthController authController;

        private UserResponse testUserResponse;
        private static final String TEST_EMAIL = "test@example.com";
        private static final String TEST_PASSWORD = "password123";
        private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

        @BeforeEach
        void setUp() {
                testUserResponse = new UserResponse();
                testUserResponse.setId(1L);
                testUserResponse.setEmail(TEST_EMAIL);
                testUserResponse.setFirstName("John");
                testUserResponse.setLastName("Doe");
                testUserResponse.setPhoneNumber("1234567890");
                testUserResponse.setStatus(AccountStatus.ACTIVE);
                testUserResponse.setRoles(Set.of(Role.USER));
                testUserResponse.setCreatedAt(Instant.now());
        }

        @Nested
        @DisplayName("login Tests")
        class LoginTests {

                @Test
                @DisplayName("Should login successfully and return token")
                void login_WithValidCredentials_ShouldReturnToken() {
                        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
                        Authentication authentication = mock(Authentication.class);
                        UserDetails userDetails = mock(UserDetails.class);

                        when(authentication.getPrincipal()).thenReturn(userDetails);
                        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                                        .when(authentication).getAuthorities();
                        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                        .thenReturn(authentication);
                        when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUserResponse);
                        when(jwtService.generateToken(eq(userDetails), eq(TEST_EMAIL), eq(1L))).thenReturn(TEST_TOKEN);

                        ResponseEntity<AuthController.LoginResponse> response = authController.login(request);

                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        AuthController.LoginResponse body = response.getBody();
                        assertThat(body).isNotNull();
                        assertThat(body.accessToken()).isEqualTo(TEST_TOKEN);
                        assertThat(body.user().email()).isEqualTo(TEST_EMAIL);
                }

                @Test
                @DisplayName("Should throw BadCredentialsException for invalid credentials")
                void login_WithInvalidCredentials_ShouldThrowException() {
                        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrong");
                        when(authenticationManager.authenticate(any()))
                                        .thenThrow(new BadCredentialsException("Invalid"));

                        assertThatThrownBy(() -> authController.login(request))
                                        .isInstanceOf(BadCredentialsException.class);
                }
        }

        @Nested
        @DisplayName("getCurrentUser Tests")
        class GetCurrentUserTests {

                @Test
                @DisplayName("Should return user info from headers when present")
                void getCurrentUser_WithHeaders_ShouldReturnUserInfo() {
                        when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUserResponse);

                        ResponseEntity<AuthController.TokenVerificationResponse> response = authController
                                        .getCurrentUser(
                                                        null, TEST_EMAIL, "1", "ROLE_USER");

                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        AuthController.TokenVerificationResponse body = response.getBody();
                        assertThat(body).isNotNull();
                        assertThat(body.valid()).isTrue();
                        assertThat(body.user().email()).isEqualTo(TEST_EMAIL);
                        assertThat(body.user().roles()).contains("ROLE_USER");
                }

                @Test
                @DisplayName("Should return user info from JWT authentication when headers absent")
                void getCurrentUser_WithJwtAuth_ShouldReturnUserInfo() {
                        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
                        Jwt jwt = mock(Jwt.class);
                        when(authentication.getPrincipal()).thenReturn(jwt);
                        when(jwt.getClaim("email")).thenReturn(TEST_EMAIL);
                        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                                        .when(authentication).getAuthorities();
                        when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUserResponse);

                        ResponseEntity<AuthController.TokenVerificationResponse> response = authController
                                        .getCurrentUser(
                                                        authentication, null, null, null);

                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                        AuthController.TokenVerificationResponse body = response.getBody();
                        assertThat(body).isNotNull();
                        assertThat(body.valid()).isTrue();
                        assertThat(body.user().email()).isEqualTo(TEST_EMAIL);
                }

                @Test
                @DisplayName("Should return 401 when no authentication provided")
                void getCurrentUser_WithNoAuth_ShouldReturn401() {
                        ResponseEntity<AuthController.TokenVerificationResponse> response = authController
                                        .getCurrentUser(
                                                        null, null, null, null);

                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        AuthController.TokenVerificationResponse body = response.getBody();
                        assertThat(body).isNotNull();
                        assertThat(body.valid()).isFalse();
                }

                @Test
                @DisplayName("Should return 401 when user not found")
                void getCurrentUser_WhenUserNotFound_ShouldReturn401() {
                        when(userService.findByEmail(TEST_EMAIL)).thenReturn(null);

                        ResponseEntity<AuthController.TokenVerificationResponse> response = authController
                                        .getCurrentUser(
                                                        null, TEST_EMAIL, "1", "ROLE_USER");

                        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                        AuthController.TokenVerificationResponse body = response.getBody();
                        assertThat(body).isNotNull();
                        assertThat(body.message()).contains("User not found");
                }
        }
}
