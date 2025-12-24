package in.winvestco.marketservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Market Service.
 * Permits public access to market data endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - market data should be accessible without authentication
                        .requestMatchers("/api/v1/market/**").permitAll()
                        .requestMatchers("/api/v1/candles/**").permitAll()
                        // Actuator endpoints for health checks
                        .requestMatchers("/actuator/**").permitAll()
                        // WebSocket endpoints
                        .requestMatchers("/ws/**").permitAll()
                        // All other requests require authentication
                        .anyRequest().permitAll());

        return http.build();
    }
}
