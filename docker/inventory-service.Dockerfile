FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY inventory-service ./inventory-service

RUN chmod +x gradlew && ./gradlew :inventory-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/inventory-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
