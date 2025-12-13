# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy parent pom and module pom
COPY pom.xml ./
COPY common/pom.xml ./common/
COPY funds-service/pom.xml ./funds-service/

# Copy source code
COPY common/src ./common/src
COPY funds-service/src ./funds-service/src

# Build the application
RUN apk add --no-cache maven && \
    mvn clean package -pl funds-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=builder /app/funds-service/target/*.jar app.jar

# Expose port
EXPOSE 8086

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
