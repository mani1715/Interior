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

# Install curl for healthcheck and add AWS RDS Global CA Bundle for sslmode=verify-full
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    curl -fsSL https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -o /usr/local/share/ca-certificates/aws-rds-global-bundle.crt && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Security: run as unprivileged non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy built artifact
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://127.0.0.1:8080/api/v1/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
