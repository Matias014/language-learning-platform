FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -q -DskipTests package
RUN JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)" && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /app/app.jar app.jar
COPY --chown=spring:spring photos photos

RUN mkdir -p /app/photos/avatars /app/photos/achievements /app/photos/country && chown -R spring:spring /app/photos

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
