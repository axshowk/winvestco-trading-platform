package in.winvestco.apigateway.filter;

import in.winvestco.apigateway.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter
 * Tests JWT processing in the API Gateway
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

        @Mock
        private JwtDecoder jwtDecoder;

        @Mock
        private WebFilterChain filterChain;

        private JwtAuthenticationFilter jwtAuthenticationFilter;

        private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid.token";
        private static final String TEST_EMAIL = "test@example.com";
        private static final String TEST_USER_ID = "1";

        @BeforeEach
        void setUp() {
                jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtDecoder);
        }

        @Nested
        @DisplayName("Public Endpoint Tests")
        class PublicEndpointTests {

                @Test
                @DisplayName("Should pass through login endpoint without JWT processing")
                void filter_LoginEndpoint_ShouldPassThrough() {
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/auth/login")
                                        .build();
                        ServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

                        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                        .verifyComplete();

                        verify(jwtDecoder, never()).decode(anyString());
                        verify(filterChain).filter(exchange);
                }

                @Test
                @DisplayName("Should pass through register endpoint without JWT processing")
                void filter_RegisterEndpoint_ShouldPassThrough() {
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .post("/api/users/register")
                                        .build();
                        ServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

                        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                        .verifyComplete();

                        verify(jwtDecoder, never()).decode(anyString());
                }

                @Test
                @DisplayName("Should pass through actuator endpoints without JWT processing")
                void filter_ActuatorEndpoint_ShouldPassThrough() {
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/actuator/health")
                                        .build();
                        ServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

                        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                        .verifyComplete();

                        verify(jwtDecoder, never()).decode(anyString());
                }
        }

        @Nested
        @DisplayName("JWT Processing Tests")
        class JwtProcessingTests {

                @Test
                @DisplayName("Should decode valid JWT and set security context")
                void filter_WithValidJwt_ShouldDecodeAndSetContext() {
                        Jwt jwt = createMockJwt();

                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/users/profile")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                        .build();
                        ServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(jwtDecoder.decode(VALID_TOKEN)).thenReturn(jwt);
                        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

                        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                        .verifyComplete();

                        verify(jwtDecoder).decode(VALID_TOKEN);
                        verify(filterChain).filter(any(ServerWebExchange.class));
                }

                @Test
                @DisplayName("Should add X-User-* headers for downstream services")
                void filter_WithValidJwt_ShouldAddUserHeaders() {
                        Jwt jwt = createMockJwt();

                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/users/profile")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                                        .build();
                        ServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(jwtDecoder.decode(VALID_TOKEN)).thenReturn(jwt);
                        when(filterChain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
                                ServerWebExchange modifiedExchange = invocation.getArgument(0);
                                ServerHttpRequest modifiedRequest = modifiedExchange.getRequest();

                                // Verify headers were added
                                assertThat(modifiedRequest.getHeaders().getFirst("X-User-Email")).isEqualTo(TEST_EMAIL);
                                assertThat(modifiedRequest.getHeaders().getFirst("X-User-Id")).isEqualTo(TEST_USER_ID);
                                assertThat(modifiedRequest.getHeaders().getFirst("X-User-Roles")).contains("ROLE_USER");

                                return Mono.empty();
                        });

                        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should continue chain for invalid JWT (let Spring Security handle)")
                void filter_WithInvalidJwt_ShouldContinueChain() {
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/users/profile")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                                        .build();
                        ServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(jwtDecoder.decode("invalid-token")).thenThrow(new JwtException("Invalid token"));
                        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

                        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                        .verifyComplete();

                        verify(filterChain).filter(exchange);
                }

                @Test
                @DisplayName("Should continue without JWT when no Authorization header")
                void filter_WithoutAuthHeader_ShouldContinueChain() {
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/users/profile")
                                        .build();
                        ServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

                        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                                        .verifyComplete();

                        verify(jwtDecoder, never()).decode(anyString());
                        verify(filterChain).filter(exchange);
                }
        }

        @Nested
        @DisplayName("isPublicEndpoint Tests")
        class IsPublicEndpointTests {

                @Test
                @DisplayName("Should identify login as public endpoint")
                void isPublicEndpoint_Login_ShouldReturnTrue() {
                        // This tests the private method indirectly
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .post("/login")
                                        .build();
                        ServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

                        jwtAuthenticationFilter.filter(exchange, filterChain);

                        verify(jwtDecoder, never()).decode(anyString());
                }

                @Test
                @DisplayName("Should identify oauth2 as public endpoint")
                void isPublicEndpoint_OAuth2_ShouldReturnTrue() {
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/oauth2/authorization/google")
                                        .build();
                        ServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

                        jwtAuthenticationFilter.filter(exchange, filterChain);

                        verify(jwtDecoder, never()).decode(anyString());
                }
        }

        private Jwt createMockJwt() {
                return Jwt.withTokenValue(VALID_TOKEN)
                                .header("alg", "HS256")
                                .claim("email", TEST_EMAIL)
                                .claim("userId", Long.parseLong(TEST_USER_ID))
                                .claim("roles", List.of("ROLE_USER"))
                                .claim("sub", TEST_EMAIL)
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .build();
        }
}
