package in.winvestco.apigateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Configuration for JWT decoding in the API Gateway
 * This allows the gateway to validate JWT tokens issued by the user-service
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret:winvestco-super-secret-key-for-jwt-token-generation-minimum-256-bits}")
    private String secret;

    @Bean
    public JwtDecoder jwtDecoder() {
        return new CustomJwtDecoder(secret);
    }

    /**
     * Custom JWT decoder that uses JJWT library to decode and validate tokens
     */
    private static class CustomJwtDecoder implements JwtDecoder {
        private final SecretKey signingKey;

        public CustomJwtDecoder(String secret) {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }

        @Override
        public Jwt decode(String token) throws JwtException {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                Map<String, Object> headers = Map.of(
                        "alg", "HS256",
                        "typ", "JWT");

                Instant issuedAt = claims.getIssuedAt() != null
                        ? claims.getIssuedAt().toInstant()
                        : Instant.now();

                Instant expiresAt = claims.getExpiration() != null
                        ? claims.getExpiration().toInstant()
                        : Instant.now().plusSeconds(86400);

                return new Jwt(
                        token,
                        issuedAt,
                        expiresAt,
                        headers,
                        claims);

            } catch (io.jsonwebtoken.JwtException e) {
                throw new JwtException("Failed to decode JWT token: " + e.getMessage(), e);
            }
        }
    }
}
