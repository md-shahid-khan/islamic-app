# Multi-stage Dockerfile for Angus Masail Spring Boot Backend

# Stage 1: Build application with Gradle
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /workspace/app

# Copy configuration and source files
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Build production jar
RUN gradle bootJar -x test --no-daemon

# Stage 2: Minimal, secure JRE 21 runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create unprivileged group and user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Create uploads directory and set permissions
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Copy the built jar from builder stage
COPY --from=builder /workspace/app/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

# Production JVM flags optimized for container memory constraints
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
