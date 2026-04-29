#!/bin/bash
# Spring Boot Application Startup Script with Memory Settings
# Adjust memory settings based on your system's available RAM

# Minimum heap size: 2048MB
# Maximum heap size: 8192MB (8GB)
# Max metaspace: 1024MB
# Enable heap dump on OOM for debugging
JVM_ARGS="-Xms2048m -Xmx8192m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heap-dump.hprof"

echo "Starting Spring Boot Application with memory settings:"
echo "$JVM_ARGS"
echo ""

# Pass JVM arguments directly to Maven
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="$JVM_ARGS"

