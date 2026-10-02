# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Install Maven
RUN apk add --no-cache maven

# Copy POM files first for layer caching
COPY pom.xml .
COPY common-events/pom.xml common-events/
COPY reporting-service/pom.xml reporting-service/

# Download dependencies (cached layer)
RUN mvn dependency:resolve -pl common-events,reporting-service --no-transfer-progress -q

# Copy source
COPY common-events/src common-events/src
COPY reporting-service/src reporting-service/src

# Build the JAR
RUN mvn clean package -pl common-events,reporting-service -am -DskipTests --no-transfer-progress -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from builder
COPY --from=builder /workspace/reporting-service/target/reporting-service-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8084/actuator/health || exit 1

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
