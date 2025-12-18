package in.winvestco.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter to extract JWT tokens from Authorization header and set authentication
 * context
 * This enables the gateway to support both OAuth2 session-based auth and JWT
 * token-based auth
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtDecoder jwtDecoder;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public @org.springframework.lang.NonNull Mono<Void> filter(
            @org.springframework.lang.NonNull ServerWebExchange exchange,
            @org.springframework.lang.NonNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip JWT processing for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Decode and validate JWT
                Jwt jwt = jwtDecoder.decode(token);

                // Extract user information from JWT
                String email = jwt.getClaimAsString("email");
                Object userIdObj = jwt.getClaim("userId");
                String userId = userIdObj != null ? userIdObj.toString() : null;

                List<String> roles = jwt.getClaim("roles");

                List<GrantedAuthority> authorities = roles != null
                        ? roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList())
                        : List.of();

                // Create authentication token
                Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
                SecurityContext context = new SecurityContextImpl(authentication);

                log.debug("JWT authentication successful for user: {}", email);

                // Add custom headers to forward user information to downstream services
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Email", email != null ? email : "")
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-User-Roles", roles != null ? String.join(",", roles) : "")
                        .build();

                ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

                // Set security context and continue with mutated exchange
                return chain.filter(mutatedExchange)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));

            } catch (JwtException e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                // Continue without setting authentication - let Spring Security handle it
                return chain.filter(exchange);
            }
        }

        // No JWT token found, continue with normal flow (OAuth2 session-based auth)
        return chain.filter(exchange);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/login") ||
                path.startsWith("/logout") ||
                path.startsWith("/oauth2") ||
                path.startsWith("/api/users/register") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/actuator") ||
                path.startsWith("/error") ||
                path.startsWith("/webjars");
    }
}
