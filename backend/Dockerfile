# Multi-stage build for Spring Boot backend
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime with Python and Docker CLI
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install Python3 and Docker CLI (CRITICAL for fault_agent.py)
RUN apk add --no-cache \
    python3 \
    py3-pip \
    docker-cli \
    curl \
    bash

# Create symbolic link for python command
RUN ln -sf /usr/bin/python3 /usr/bin/python

# Copy JAR from build stage
COPY --from=build /build/target/*.jar /app/app.jar

# Create directory for backend scripts
RUN mkdir -p /app/backend

# Expose Spring Boot port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run Spring Boot application
ENTRYPOINT ["java"]
CMD ["-jar", "/app/app.jar"]
