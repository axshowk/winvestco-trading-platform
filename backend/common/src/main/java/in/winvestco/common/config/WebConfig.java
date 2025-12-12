package in.winvestco.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import in.winvestco.common.interceptor.RateLimitInterceptor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**") // Apply rate limiting to all API endpoints
                .excludePathPatterns(
                    "/api/auth/login", // Exclude login endpoint from rate limiting
                    "/api/auth/register", // Exclude registration endpoint from rate limiting
                    "/actuator/**", // Exclude health check endpoints
                    "/swagger-ui/**", // Exclude Swagger UI
                    "/v3/api-docs/**" // Exclude OpenAPI docs
                );
    }
}
