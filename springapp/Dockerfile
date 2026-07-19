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

ENV PORT=8080

# 512 MB cgroup budget (all regions share the same physical limit):
#   Heap .............. ~220 MB  (MaxRAMPercentage=43)
#   Metaspace ......... ~144 MB  (MaxMetaspaceSize — class metadata for 126 repos + Hibernate)
#   Code cache ........ ~48 MB
#   Thread stacks ....... ~13 MB  (25 threads × 512 kB)
#   Direct / native ... ~50 MB   (HikariCP, MySQL driver, NIO)
#   OS / GC overhead .... ~37 MB
#
# NEVER set MaxMetaspaceSize=512m on a 512 MB container — metaspace + heap exceed cgroup RAM.
ENV JAVA_OPTS="\
-XX:+UseContainerSupport \
-XX:MaxRAMPercentage=43.0 \
-XX:MaxMetaspaceSize=144m \
-XX:CompressedClassSpaceSize=48m \
-XX:ReservedCodeCacheSize=48m \
-XX:MinMetaspaceFreeRatio=10 \
-XX:MaxMetaspaceFreeRatio=70 \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:+UseStringDeduplication \
-XX:TieredStopAtLevel=1 \
-Xss512k \
-XX:+ExitOnOutOfMemoryError \
-Djava.security.egd=file:/dev/./urandom \
-Dspring.backgroundpreinitializer.ignore=true \
-Dfile.encoding=UTF-8"

EXPOSE 8080

CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
