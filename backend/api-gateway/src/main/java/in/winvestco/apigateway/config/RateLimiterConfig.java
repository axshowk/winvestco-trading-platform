package in.winvestco.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Rate Limiter Configuration for API Gateway.
 * Provides KeyResolver beans for different rate limiting strategies:
 * - IP-based rate limiting (default)
 * - User-based rate limiting for authenticated requests
 */
@Slf4j
@Configuration
public class RateLimiterConfig {

    /**
     * Primary KeyResolver that uses client IP address.
     * Extracts IP from X-Forwarded-For header (for proxied requests) or remote
     * address.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = Optional.ofNullable(
                    exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"))
                    .map(header -> header.split(",")[0].trim())
                    .orElseGet(() -> {
                        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
                        return remoteAddress != null
                                ? remoteAddress.getAddress().getHostAddress()
                                : "unknown";
                    });
            log.debug("Rate limiting by IP: {}", ip);
            return Mono.just(ip);
        };
    }

    /**
     * User-based KeyResolver for authenticated requests.
     * Uses the X-User-Id header (set by JwtAuthenticationFilter) or falls back to
     * IP.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                log.debug("Rate limiting by user ID: {}", userId);
                return Mono.just("user:" + userId);
            }
            // Fall back to IP if no user ID
            return ipKeyResolver().resolve(exchange);
        };
    }

    /**
     * Combined KeyResolver for defense in depth.
     * Uses both user ID and IP for more granular rate limiting.
     */
    @Bean
    public KeyResolver combinedKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            String ip = Optional.ofNullable(
                    exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"))
                    .map(header -> header.split(",")[0].trim())
                    .orElseGet(() -> {
                        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
                        return remoteAddress != null
                                ? remoteAddress.getAddress().getHostAddress()
                                : "unknown";
                    });

            String key = (userId != null && !userId.isEmpty())
                    ? "user:" + userId + ":ip:" + ip
                    : "ip:" + ip;
            log.debug("Rate limiting by combined key: {}", key);
            return Mono.just(key);
        };
    }
}
