@echo off
REM Spring Boot Application Startup Script with Memory Settings
REM Adjust memory settings based on your system's available RAM

REM Use JDK 25 (latest LTS) for compilation and runtime
set JAVA_HOME=C:\Users\sairo\.jdk\jdk-25(1)
set PATH=%JAVA_HOME%\bin;%PATH%

REM Minimum heap size: 2048MB
REM Maximum heap size: 8192MB (8GB)
REM Max metaspace: 1024MB
REM Enable heap dump on OOM for debugging
set JVM_ARGS=-Xms2048m -Xmx8192m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heap-dump.hprof

echo Starting Spring Boot Application with memory settings:
echo %JVM_ARGS%
echo.

REM Pass JVM arguments directly to Maven
mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="%JVM_ARGS%"

q