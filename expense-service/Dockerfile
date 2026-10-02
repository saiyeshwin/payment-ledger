# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Install Maven
RUN apk add --no-cache maven

# Copy POM files first for layer caching
COPY pom.xml .
COPY common-events/pom.xml common-events/
COPY expense-service/pom.xml expense-service/

# Download dependencies (cached layer)
RUN mvn dependency:resolve -pl common-events,expense-service --no-transfer-progress -q

# Copy source
COPY common-events/src common-events/src
COPY expense-service/src expense-service/src

# Build the JAR
RUN mvn clean package -pl common-events,expense-service -am -DskipTests --no-transfer-progress -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from builder
COPY --from=builder /workspace/expense-service/target/expense-service-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8082/actuator/health || exit 1

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
