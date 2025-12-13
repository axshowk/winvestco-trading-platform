FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY market-service/pom.xml market-service/pom.xml
COPY portfolio-service/pom.xml portfolio-service/pom.xml
RUN mvn dependency:go-offline -pl portfolio-service -am
COPY common/src common/src
COPY portfolio-service/src portfolio-service/src
RUN mvn -pl portfolio-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/portfolio-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
