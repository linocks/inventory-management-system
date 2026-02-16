FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY common ./common
COPY reporting-service ./reporting-service

RUN chmod +x gradlew && ./gradlew :reporting-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/reporting-service/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
