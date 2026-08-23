# ---- Build stage ----
# Use official Maven image so Render does not download Maven 4.0.0-rc-5
# (that Apache URL is gone and caused wget + mvnw exit code 1).
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY . .

# Local jvm.config requests 1–4 GB heap; Render build VMs cannot use that.
RUN printf '%s\n' '-Xmx512m' > .mvn/jvm.config \
    && mvn -B clean package -DskipTests -Dspring-boot.repackage.excludeDevtools=true

# ---- Runtime stage (JRE only — smaller image, less native memory) ----
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=build /workspace/target/*.jar app.jar

RUN mkdir -p /app/uploads \
    && chown -R appuser:appuser /app/app.jar /app/uploads

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
