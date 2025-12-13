FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY market-service/pom.xml market-service/pom.xml
COPY portfolio-service/pom.xml portfolio-service/pom.xml
COPY funds-service/pom.xml funds-service/pom.xml
COPY ledger-service/pom.xml ledger-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
RUN mvn dependency:go-offline -pl order-service -am
COPY common/src common/src
COPY order-service/src order-service/src
RUN mvn -pl order-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
