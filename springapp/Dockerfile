# ---- Build stage ----
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY . .

RUN chmod +x mvnw \
    && ./mvnw clean package -DskipTests -Dspring-boot.repackage.excludeDevtools=true

# ---- Runtime stage (JRE only — smaller image, less native memory) ----
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=build /workspace/target/*.jar app.jar

RUN chown appuser:appuser app.jar

USER appuser

# Render injects PORT at runtime; application.properties binds server.port=${PORT:8080}
ENV PORT=8080

# Keep enough headroom for Spring Boot class loading and generated proxy classes.
# The previous 96m cap was too small for this app's dependency footprint.
ENV JAVA_OPTS="\
-XX:+UseContainerSupport \
-XX:InitialRAMPercentage=25.0 \
-XX:MaxRAMPercentage=70.0 \
-XX:MetaspaceSize=256m \
-XX:MaxMetaspaceSize=512m \
-XX:ReservedCodeCacheSize=256m \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:+UseStringDeduplication \
-XX:TieredStopAtLevel=1 \
-Djava.security.egd=file:/dev/./urandom \
-Dspring.backgroundpreinitializer.ignore=true \
-Dfile.encoding=UTF-8"

EXPOSE 8080

# Bind to Render's PORT env var via Spring Boot (never hardcode server.port here)
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
