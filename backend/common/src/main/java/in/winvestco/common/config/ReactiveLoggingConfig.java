package in.winvestco.common.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import org.springframework.web.server.WebFilter;

import in.winvestco.common.util.LoggingUtils;

import java.util.UUID;

/**
 * Reactive logging configuration for WebFlux applications like the API Gateway.
 * Provides consistent logging setup, request tracing, and structured logging.
 * Only active for reactive (WebFlux) applications.
 */
@Configuration("reactiveLoggingConfig")
@ConditionalOnWebApplication(type = Type.REACTIVE)
public class ReactiveLoggingConfig {
    private static final Logger log = LoggerFactory.getLogger(ReactiveLoggingConfig.class);

    @Value("${spring.application.name:BInvestco-Service}")
    private String serviceName;

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String SERVICE_NAME_MDC_KEY = "serviceName";
    private static final String USER_ID_MDC_KEY = "userId";

    @Bean
    @Order(-2) // Ensure this runs before other filters
    public WebFilter reactiveRequestContextFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // Generate or retrieve request ID for tracing
            String requestHeader = request.getHeaders().getFirst(REQUEST_ID_HEADER);
            final String requestId = (requestHeader != null && !requestHeader.trim().isEmpty())
                    ? requestHeader
                    : UUID.randomUUID().toString();

            final String userId = request.getHeaders().getFirst(USER_ID_HEADER);

            // Log incoming request
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            MDC.put(SERVICE_NAME_MDC_KEY, serviceName);
            if (userId != null) {
                MDC.put(USER_ID_MDC_KEY, userId);
            }

            log.info("Incoming request: {} {} from {} - RequestId: {}",
                    request.getMethod(), request.getPath(),
                    request.getRemoteAddress(), requestId);

            // Add request ID to response headers
            response.getHeaders().set(REQUEST_ID_HEADER, requestId);

            // Continue with the filter chain
            return chain.filter(exchange)
                    .doFinally(signalType -> {
                        // Log response
                        log.info("Completed request: {} {} - Status: {} - RequestId: {}",
                                request.getMethod(), request.getPath(),
                                response.getStatusCode(), requestId);

                        // Clear MDC context
                        MDC.clear();
                    });
        };
    }

    @Bean
    public LoggingUtils loggingUtils() {
        LoggingUtils utils = new LoggingUtils();
        utils.setServiceName(serviceName);
        log.info("Creating reactive LoggingUtils bean for service: {}", serviceName);
        return utils;
    }
}
