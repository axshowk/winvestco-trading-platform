package in.winvestco.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class GlobalCorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Explicitly list allowed origins (adjust these to match your frontend URLs)
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000", // React default port
                "http://localhost:4200", // Angular default port
                "http://localhost:8080", // Common frontend port
                "http://localhost:5173" // Vite default port
        ));

        // Allowed HTTP methods
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allowed headers
        config.setAllowedHeaders(Arrays.asList(
                "Origin", "Content-Type", "Accept", "Authorization",
                "X-Requested-With", "X-Auth-Token", "X-Request-Id", "X-Correlation-Id"));

        // Exposed headers
        config.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With",
                "X-Auth-Token", "X-Request-Id", "X-Correlation-Id"));

        // Cache preflight requests for 1 hour
        config.setMaxAge(3600L);

        // Apply CORS configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
