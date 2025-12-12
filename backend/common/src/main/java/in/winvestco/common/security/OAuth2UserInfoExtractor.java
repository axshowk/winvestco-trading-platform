package in.winvestco.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2UserInfoExtractor {

    public Optional<String> getCurrentUserId() {
        return getClaim("sub");
    }

    public Optional<String> getClaim(String claimName) {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast)
                .map(jwt -> jwt.getClaimAsString(claimName));
    }

    public Map<String, Object> getClaims() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .map(authentication -> (JwtAuthenticationToken) authentication)
                .map(JwtAuthenticationToken::getToken)
                .map(Jwt::getClaims)
                .orElse(Collections.emptyMap());
    }
}
