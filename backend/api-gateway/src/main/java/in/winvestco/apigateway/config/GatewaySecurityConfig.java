package in.winvestco.apigateway.config;

import in.winvestco.apigateway.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

        private final ReactiveClientRegistrationRepository clientRegistrationRepository;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
                                                                "/api/auth/login",
                                                                "/api/users/register",
                                                                "/api/users/register/**",
                                                                "/api/market/**",
                                                                "/api/v1/market/**",
                                                                "/api/v1/candles/**",
                                                                "/ws/**")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .oauth2Login(oauth2 -> {
                                })
                                .logout(logout -> logout
                                                .logoutSuccessHandler(oidcLogoutSuccessHandler()))
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                // Add JWT authentication filter before OAuth2 login filter
                                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

                return http.build();
        }

        private ServerLogoutSuccessHandler oidcLogoutSuccessHandler() {
                OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedServerLogoutSuccessHandler(
                                this.clientRegistrationRepository);
                oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/login?logout");
                return oidcLogoutSuccessHandler;
        }
}
