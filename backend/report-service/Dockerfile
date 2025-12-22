# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy maven wrapper and pom files
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY common/pom.xml common/
COPY report-service/pom.xml report-service/

# Download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -pl report-service -am -B

# Copy source code
COPY common/src common/src
COPY report-service/src report-service/src

# Build the application
RUN ./mvnw package -pl report-service -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create reports directory
RUN mkdir -p /app/reports

# Copy jar from builder
COPY --from=builder /app/report-service/target/*.jar app.jar

# Expose port
EXPOSE 8094

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8094/actuator/health || exit 1

# Run the application with virtual threads
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "--enable-preview", "-jar", "app.jar"]
