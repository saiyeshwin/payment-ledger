# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Install Maven
RUN apk add --no-cache maven

# Copy POM files first for layer caching
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/

# Download dependencies (cached layer)
RUN mvn dependency:resolve -pl api-gateway --no-transfer-progress -q

# Copy source
COPY api-gateway/src api-gateway/src

# Build the JAR
RUN mvn clean package -pl api-gateway -am -DskipTests --no-transfer-progress -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from builder
COPY --from=builder /workspace/api-gateway/target/api-gateway-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
