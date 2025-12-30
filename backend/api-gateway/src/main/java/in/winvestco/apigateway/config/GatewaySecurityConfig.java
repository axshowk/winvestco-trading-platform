package in.winvestco.apigateway.config;

import in.winvestco.apigateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.PortMapperImpl;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;

import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

        private final ReactiveClientRegistrationRepository clientRegistrationRepository;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Value("${security.enforce-https:false}")
        private boolean enforceHttps;

        @Value("${server.port:8090}")
        private int serverPort;

        public GatewaySecurityConfig(
                        ReactiveClientRegistrationRepository clientRegistrationRepository,
                        JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.clientRegistrationRepository = clientRegistrationRepository;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
                http
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers(
                                                                "/actuator/**",
                                                                "/login",
                                                                "/logout",
                                                                "/oauth2/**",
                                                                "/webjars/**",
                                                                "/error",
                                                                "/api/v1/auth/login",
                                                                "/api/v1/users/register",
                                                                "/api/v1/users/register/**",
                                                                "/api/v1/market/**",
                                                                "/api/v1/candles/**",
                                                                "/ws/**")
                                                .permitAll()
                                                .pathMatchers(org.springframework.http.HttpMethod.OPTIONS).permitAll()
                                                .anyExchange().authenticated())
                                .oauth2Login(oauth2 -> {
                                })
                                .logout(logout -> logout
                                                .logoutSuccessHandler(oidcLogoutSuccessHandler()))
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .headers(headers -> headers
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives("default-src 'self'; " +
                                                                                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://www.google.com https://www.gstatic.com; "
                                                                                +
                                                                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                                                                +
                                                                                "font-src 'self' https://fonts.gstatic.com; "
                                                                                +
                                                                                "img-src 'self' data: https://www.google.com https://www.gstatic.com; "
                                                                                +
                                                                                "connect-src 'self' ws: wss: https://www.nseindia.com; "
                                                                                +
                                                                                "frame-src 'self' https://www.google.com;"))
                                                .frameOptions(frameOptions -> frameOptions
                                                                .mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                                                .hsts(hsts -> hsts
                                                                .includeSubdomains(true)
                                                                .maxAge(java.time.Duration.ofDays(365)))
                                                .referrerPolicy(referrer -> referrer
                                                                .policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                                .permissionsPolicy(permissions -> permissions
                                                                .policy("camera=(), microphone=(), geolocation=(), payment=()")))
                                // Add JWT authentication filter before OAuth2 login filter
                                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

                if (enforceHttps) {
                        PortMapperImpl portMapper = new PortMapperImpl();
                        portMapper.setPortMappings(Collections.singletonMap(String.valueOf(serverPort), "443"));
                        http.redirectToHttps(redirect -> redirect.portMapper(portMapper));
                }

                return http.build();
        }

        private ServerLogoutSuccessHandler oidcLogoutSuccessHandler() {
                OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedServerLogoutSuccessHandler(
                                this.clientRegistrationRepository);
                oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/login?logout");
                return oidcLogoutSuccessHandler;
        }
}
