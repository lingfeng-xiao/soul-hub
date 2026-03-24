# =============================================================================
# Sprite Digital Being - Docker Containerization
# Multi-stage build for Java 21 Spring Boot application
# Supports: AMD64 (x86_64), ARM64 (aarch64)
# =============================================================================

# -----------------------------------------------------------------------------
# Build Stage
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /build

# Copy Maven dependency files first (for layer caching)
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for production build)
RUN mvn package -DskipTests

# -----------------------------------------------------------------------------
# Runtime Stage
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime

# Labels for container metadata
LABEL org.opencontainers.image.title="Sprite Digital Being"
LABEL org.opencontainers.image.description="数字生命 - Digital Being AI Agent"
LABEL org.opencontainers.image.version="0.1.0"
LABEL org.opencontainers.image.source="https://github.com/lingfeng-bbben/sprite"
LABEL org.opencontainers.image.architecture="${TARGETARCH}"

# Install curl for health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r sprite && useradd -r -g sprite sprite

# Set working directory
WORKDIR /app

# Create directories for external configuration and data
RUN mkdir -p /app/config /app/data /app/logs && chown -R sprite:sprite /app

# Switch to non-root user
USER sprite

# Copy the built JAR from builder stage
COPY --from=builder /build/target/sprite-*.jar app.jar

# Set JVM defaults for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Expose application port
EXPOSE 8080

# Health check configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Entry point with production profile
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar -Dspring.profiles.active=prod app.jar"]
