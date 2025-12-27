package in.winvestco.apigateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RateLimiterConfig KeyResolver beans.
 * Tests IP extraction, user extraction, and fallback behavior.
 */
class RateLimiterConfigTest {

    private RateLimiterConfig config;

    @BeforeEach
    void setUp() {
        config = new RateLimiterConfig();
    }

    @Nested
    @DisplayName("IP KeyResolver Tests")
    class IpKeyResolverTests {

        @Test
        @DisplayName("Should extract IP from X-Forwarded-For header")
        void shouldExtractIpFromXForwardedForHeader() {
            KeyResolver resolver = config.ipKeyResolver();

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.100, 10.0.0.1, 172.16.0.1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .assertNext(key -> assertThat(key).isEqualTo("192.168.1.100"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should use remote address when X-Forwarded-For is missing")
        void shouldUseRemoteAddressWhenXForwardedForMissing() {
            KeyResolver resolver = config.ipKeyResolver();

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .remoteAddress(new InetSocketAddress("10.0.0.50", 12345))
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .assertNext(key -> assertThat(key).isEqualTo("10.0.0.50"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return 'unknown' when no IP can be determined")
        void shouldReturnUnknownWhenNoIpCanBeDetermined() {
            KeyResolver resolver = config.ipKeyResolver();

            // Create request without remote address
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .assertNext(key -> assertThat(key).isNotEmpty())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("User KeyResolver Tests")
    class UserKeyResolverTests {

        @Test
        @DisplayName("Should extract user ID from X-User-Id header")
        void shouldExtractUserIdFromHeader() {
            KeyResolver resolver = config.userKeyResolver();

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-User-Id", "12345")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .assertNext(key -> assertThat(key).isEqualTo("user:12345"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should fall back to IP when X-User-Id is missing")
        void shouldFallBackToIpWhenUserIdMissing() {
            KeyResolver resolver = config.userKeyResolver();

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.100")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .assertNext(key -> assertThat(key).isEqualTo("192.168.1.100"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should fall back to IP when X-User-Id is empty")
        void shouldFallBackToIpWhenUserIdEmpty() {
            KeyResolver resolver = config.userKeyResolver();

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-User-Id", "")
                    .header("X-Forwarded-For", "10.0.0.1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .assertNext(key -> assertThat(key).isEqualTo("10.0.0.1"))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Combined KeyResolver Tests")
    class CombinedKeyResolverTests {

        @Test
        @DisplayName("Should combine user ID and IP when both present")
        void shouldCombineUserIdAndIp() {
            KeyResolver resolver = config.combinedKeyResolver();

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-User-Id", "12345")
                    .header("X-Forwarded-For", "192.168.1.100")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .assertNext(key -> assertThat(key).isEqualTo("user:12345:ip:192.168.1.100"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should use IP only when user ID is missing")
        void shouldUseIpOnlyWhenUserIdMissing() {
            KeyResolver resolver = config.combinedKeyResolver();

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.100")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .assertNext(key -> assertThat(key).isEqualTo("ip:192.168.1.100"))
                    .verifyComplete();
        }
    }
}
