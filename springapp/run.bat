@echo off
java -jar target/springapp-0.0.1-SNAPSHOT.jar 2>&1
echo Exit code: %ERRORLEVEL%
