# =============================================================================
# WINVESTCO Trading Platform - Payment Service
# Optimized Multi-Stage Docker Build
# =============================================================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom files first for dependency caching
COPY pom.xml ./
COPY common/pom.xml common/
COPY payment-service/pom.xml payment-service/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl payment-service -am -B

# Copy source code
COPY common/src common/src
COPY payment-service/src payment-service/src

# Build the application
RUN mvn clean package -pl payment-service -am -DskipTests -B

# Stage 2: Runtime (minimal JRE image)
FROM eclipse-temurin:21-jre-alpine

# OCI Labels
LABEL org.opencontainers.image.title="WINVESTCO Payment Service" \
    org.opencontainers.image.description="Razorpay Integration for Payment Processing" \
    org.opencontainers.image.vendor="WINVESTCO" \
    org.opencontainers.image.source="https://github.com/axshowk/winvestco-trading-platform"

WORKDIR /app

# Create non-root user for security
RUN addgroup -S winvestco && adduser -S appuser -G winvestco

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy the JAR from build stage
COPY --from=build /app/payment-service/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:winvestco /app

# Switch to non-root user
USER appuser

# Expose service port
EXPOSE 8093

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8093/actuator/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application with optimized JVM settings
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
