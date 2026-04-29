@echo off
REM Spring Boot Application Run Script with Memory Settings
REM Maximum heap size: 8192MB (8GB)

REM Use JDK 25 (latest LTS)
set JAVA_HOME=C:\Users\sairo\.jdk\jdk-25(1)
set PATH=%JAVA_HOME%\bin;%PATH%

.\mvnw.cmd spring-boot:run 2>&1
echo Exit code: %ERRORLEVEL%
