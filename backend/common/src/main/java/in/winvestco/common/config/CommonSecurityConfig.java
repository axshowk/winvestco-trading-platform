package in.winvestco.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Common Security configuration - FALLBACK only.
 * This configuration is DISABLED by default and only activates when:
 * 1. Property 'security.common.enabled' is set to true
 * 2. No other SecurityFilterChain bean is already defined
 * 
 * Each service should define its own SecurityConfig with specific settings.
 */
@Configuration("commonSecurityConfig")
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(name = "security.common.enabled", havingValue = "true", matchIfMissing = false)
public class CommonSecurityConfig {

    @Bean("commonFallbackSecurityFilterChain")
    @Order(Integer.MAX_VALUE) // Lowest priority
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/users/**",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean("commonAuthenticationProvider")
    @ConditionalOnBean(UserDetailsService.class)
    @ConditionalOnMissingBean(AuthenticationProvider.class)
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        // Create the authentication provider with the required dependencies
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(passwordEncoder);
        authProvider.setUserDetailsService(userDetailsService);

        // Only set the password service if the userDetailsService implements
        // UserDetailsPasswordService
        if (userDetailsService instanceof UserDetailsPasswordService) {
            authProvider.setUserDetailsPasswordService((UserDetailsPasswordService) userDetailsService);
        }

        return authProvider;
    }

    @Bean("commonAuthenticationManager")
    @ConditionalOnBean(AuthenticationProvider.class)
    @ConditionalOnMissingBean(AuthenticationManager.class)
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
