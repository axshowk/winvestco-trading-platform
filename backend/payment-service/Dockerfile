# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common/pom.xml common/
COPY payment-service/pom.xml payment-service/

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -pl payment-service -am -B

# Copy source code
COPY common/src common/src
COPY payment-service/src payment-service/src

# Build the application
RUN ./mvnw clean package -pl payment-service -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1001 spring && adduser -u 1001 -G spring -D spring
USER spring:spring

# Copy the built jar
COPY --from=builder /app/payment-service/target/*.jar app.jar

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

EXPOSE 8093

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
