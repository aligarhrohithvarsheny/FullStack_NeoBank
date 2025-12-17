# Multi-stage Dockerfile for Spring Boot application
# Stage 1: Build stage with Maven
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the JAR from build stage
COPY --from=build /app/target/springapp-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port 8080
EXPOSE 8080

# Set JVM options for containerized environments
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application with production profile
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=production -jar app.jar"]
