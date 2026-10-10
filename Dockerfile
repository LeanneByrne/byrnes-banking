# Multi-stage: the build stage has the full JDK + Maven wrapper and is
# discarded; only the runtime stage (JRE + the built jar) ends up in the
# final image actually pushed to ECR.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src/main/ src/main/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/byrnes-banking-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
