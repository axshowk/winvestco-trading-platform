package in.winvestco.common.config;

import org.springframework.lang.Nullable;
import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import in.winvestco.common.util.LoggingUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Enhanced centralized logging configuration for all services.
 * Provides consistent logging setup, request tracing, and structured logging across the application.
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
@PropertySource(value = {
    "classpath:config/logging.properties",
    "classpath:config/common.properties"
}, ignoreResourceNotFound = true)
public class LoggingConfig implements WebMvcConfigurer {

    @Value("${spring.application.name:BInvestco-Service}")
    private String serviceName;

    public LoggingConfig() {
        log.info("Enhanced LoggingConfig initialized - centralized logging configuration loaded");
    }

    @Bean
    public LoggingUtils loggingUtils() {
        LoggingUtils utils = new LoggingUtils();
        utils.setServiceName(serviceName);
        log.info("Creating enhanced LoggingUtils bean for service: {}", serviceName);
        return utils;
    }

    @Bean
    public RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor();
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/api-docs/**");
    }

    /**
     * Request logging interceptor for automatic request/response logging and tracing
     */
    public static class RequestLoggingInterceptor implements org.springframework.web.servlet.HandlerInterceptor {

        private static final String REQUEST_ID_HEADER = "X-Request-ID";
        private static final String USER_ID_HEADER = "X-User-ID";

        @Override
        public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                               @NonNull Object handler) throws Exception {

            // Generate or retrieve request ID for tracing
           String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.trim().isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }

            // Set MDC context for structured logging
            MDC.put("requestId", requestId);
            MDC.put("serviceName", getServiceNameFromRequest(request));
            MDC.put("userId", request.getHeader(USER_ID_HEADER));

            // Log incoming request
            log.info("Incoming request: {} {} from {} - RequestId: {}",
                    request.getMethod(), request.getRequestURI(),
                    request.getRemoteAddr(), requestId);

            // Add request ID to response headers for tracing
            response.setHeader(REQUEST_ID_HEADER, requestId);

            return true;
        }

        @Override
        public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                  @NonNull Object handler, @Nullable Exception ex) throws Exception {

            // Log response
            log.info("Request completed: {} {} - Status: {} - RequestId: {}",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), MDC.get("requestId"));

            // Clear MDC context
            MDC.clear();
        }

        private String getServiceNameFromRequest(HttpServletRequest request) {
            String path = request.getRequestURI();
            if (path.contains("/auth")) return "user-service";
            if (path.contains("/accounts")) return "account-service";
            if (path.contains("/portfolios")) return "portfolio-service";
            if (path.contains("/trades")) return "trade-service";
            if (path.contains("/orders")) return "order-service";
            if (path.contains("/market")) return "market-service";
            if (path.contains("/notifications")) return "notification-service";
            if (path.contains("/reports")) return "reports-service";
            return "unknown-service";
        }
    }
}
