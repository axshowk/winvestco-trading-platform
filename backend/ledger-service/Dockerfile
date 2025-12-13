# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy parent pom and module pom
COPY pom.xml ./
COPY common/pom.xml ./common/
COPY ledger-service/pom.xml ./ledger-service/

# Copy source code
COPY common/src ./common/src
COPY ledger-service/src ./ledger-service/src

# Build the application
RUN apk add --no-cache maven && \
    mvn clean package -pl ledger-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=builder /app/ledger-service/target/*.jar app.jar

# Expose port
EXPOSE 8087

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
