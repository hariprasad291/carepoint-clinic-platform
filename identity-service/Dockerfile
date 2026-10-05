FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -pl identity-service -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app
COPY --from=build --chown=10001:10001 /workspace/identity-service/target/identity-service-0.1.0-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
