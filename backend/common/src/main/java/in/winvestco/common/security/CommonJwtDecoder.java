package in.winvestco.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Common JWT Decoder for all microservices.
 * Provides consistent JWT token parsing and validation across the platform.
 * 
 * <p>
 * Usage in service SecurityConfig:
 * 
 * <pre>
 * {@code
 * @Bean
 * public JwtDecoder jwtDecoder() {
 *     return new CommonJwtDecoder(jwtSecret);
 * }
 * }
 * </pre>
 */
public class CommonJwtDecoder implements JwtDecoder {

    private final SecretKey signingKey;

    /**
     * Creates a new CommonJwtDecoder with the specified secret.
     * 
     * @param secret the JWT signing secret (should be at least 256 bits for HS256)
     */
    public CommonJwtDecoder(String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
