# Stage 1: Build the application using Maven
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests


# Stage 2: Run the application with Tesseract OCR installed
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install Tesseract OCR and required native libraries
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        tesseract-ocr \
        libtesseract-dev \
        libleptonica-dev \
    && rm -rf /var/lib/apt/lists/* \
    && ldconfig

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "app.jar"]