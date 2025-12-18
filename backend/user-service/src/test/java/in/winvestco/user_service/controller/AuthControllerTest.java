package in.winvestco.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.util.LoggingUtils;
import in.winvestco.user_service.dto.LoginRequest;
import in.winvestco.user_service.dto.UserResponse;
import in.winvestco.user_service.service.JwtService;
import in.winvestco.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController
 * Uses @WebMvcTest for slice testing
 */
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private AuthenticationManager authenticationManager;

        @MockBean
        private UserService userService;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

        @MockBean
        private LoggingUtils loggingUtils;

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
                testUserResponse.setStatus(in.winvestco.common.enums.AccountStatus.ACTIVE);
                testUserResponse.setRoles(Set.of(in.winvestco.common.enums.Role.USER));
                testUserResponse.setCreatedAt(Instant.now());
        }

        @Test
        @DisplayName("Should return JWT token on successful login")
        void login_WithValidCredentials_ShouldReturnToken() throws Exception {
                LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

                Authentication authentication = mock(Authentication.class);
                UserDetails userDetails = User.builder()
                                .username(TEST_EMAIL)
                                .password(TEST_PASSWORD)
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .build();

                when(authentication.getPrincipal()).thenReturn(userDetails);
                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(authentication);
                when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUserResponse);
                when(jwtService.generateToken(any(UserDetails.class), any(), any())).thenReturn(TEST_TOKEN);

                mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value(TEST_TOKEN))
                                .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("Should return 401 on invalid credentials")
        void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
                LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, "wrongPassword");

                when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenThrow(new BadCredentialsException("Invalid credentials"));

                mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "test@example.com", roles = { "USER" })
        @DisplayName("Should return current user info for authenticated request")
        void getCurrentUser_WithAuthentication_ShouldReturnUserInfo() throws Exception {
                when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUserResponse);

                mockMvc.perform(get("/api/auth/me")
                                .header("X-User-Email", TEST_EMAIL)
                                .header("X-User-Id", "1")
                                .header("X-User-Roles", "ROLE_USER"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(true));
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request to protected endpoint")
        void getCurrentUser_WithoutAuthentication_ShouldReturn401() throws Exception {
                mockMvc.perform(get("/api/auth/me"))
                                .andExpect(status().isUnauthorized());
        }
}
