package in.winvestco.common.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized OpenTelemetry configuration for distributed tracing.
 * Provides tracer beans and utility methods for trace context extraction.
 */
@Slf4j
@Configuration
@ConditionalOnClass(OpenTelemetry.class)
public class OpenTelemetryConfig {

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * Creates a Tracer bean for custom span creation.
     * The OpenTelemetry instance is auto-configured by Spring Boot actuator.
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        log.info("Creating OpenTelemetry Tracer for service: {}", serviceName);
        return openTelemetry.getTracer(serviceName, "1.0.0");
    }

    /**
     * Extracts the current trace ID from the active span context.
     * Returns "00000000000000000000000000000000" if no trace is active.
     */
    public static String getCurrentTraceId() {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            SpanContext spanContext = currentSpan.getSpanContext();
            if (spanContext.isValid()) {
                return spanContext.getTraceId();
            }
        }
        return "00000000000000000000000000000000";
    }

    /**
     * Extracts the current span ID from the active span context.
     * Returns "0000000000000000" if no span is active.
     */
    public static String getCurrentSpanId() {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            SpanContext spanContext = currentSpan.getSpanContext();
            if (spanContext.isValid()) {
                return spanContext.getSpanId();
            }
        }
        return "0000000000000000";
    }

    /**
     * Checks if there is an active valid trace context.
     */
    public static boolean hasActiveTrace() {
        Span currentSpan = Span.current();
        return currentSpan != null && currentSpan.getSpanContext().isValid();
    }
}
