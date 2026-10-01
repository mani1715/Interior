# Multi-stage production Dockerfile for Interior Design Platform API
# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Production Runtime
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Security: run as unprivileged non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy built artifact
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
