FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 10000

ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC"

CMD ["java", "-Dserver.port=10000", "-jar", "app.jar"]