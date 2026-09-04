FROM maven:3.9.16-eclipse-temurin-26 AS build

WORKDIR /app
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn ./.mvn
COPY src ./src
RUN chmod +x mvnw && ./mvnw -B package -DskipTests

FROM eclipse-temurin:26-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-tur tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]