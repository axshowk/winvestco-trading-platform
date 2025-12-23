package in.winvestco.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

        @Mock
        private JwtDecoder jwtDecoder;

        @Mock
        private WebFilterChain chain;

        private JwtAuthenticationFilter filter;

        @BeforeEach
        void setUp() {
                filter = new JwtAuthenticationFilter(jwtDecoder);
        }

        @Test
        void filter_WhenPublicEndpoint_ShouldSkipProcessing() {
                MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/login").build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);
                when(chain.filter(any())).thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                                .verifyComplete();

                verifyNoInteractions(jwtDecoder);
                verify(chain).filter(exchange);
        }

        @Test
        void filter_WhenValidToken_ShouldMutateRequestAndSetContext() {
                MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer valid_token")
                                .build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);

                Jwt jwt = mock(Jwt.class);
                when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
                when(jwt.getClaim("userId")).thenReturn(1L);
                when(jwt.getClaim("roles")).thenReturn(List.of("ROLE_USER"));
                when(jwtDecoder.decode(anyString())).thenReturn(jwt);
                when(chain.filter(any())).thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                                .verifyComplete();

                verify(jwtDecoder).decode("valid_token");
                verify(chain).filter(argThat(ex -> {
                        ServerHttpRequest mutatedRequest = ex.getRequest();
                        return "test@example.com".equals(mutatedRequest.getHeaders().getFirst("X-User-Email")) &&
                                        "1".equals(mutatedRequest.getHeaders().getFirst("X-User-Id")) &&
                                        "ROLE_USER".equals(mutatedRequest.getHeaders().getFirst("X-User-Roles"));
                }));
        }

        @Test
        void filter_WhenNoToken_ShouldContinueChain() {
                MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders").build();
                MockServerWebExchange exchange = MockServerWebExchange.from(request);
                when(chain.filter(any())).thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                                .verifyComplete();

                verifyNoInteractions(jwtDecoder);
                verify(chain).filter(exchange);
        }
}
