FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/springapp-0.0.1-SNAPSHOT.jar"]
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms1024m", "-Xmx4096m", "-XX:MaxMetaspaceSize=512m", "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-jar", "/app/app.jar"]
