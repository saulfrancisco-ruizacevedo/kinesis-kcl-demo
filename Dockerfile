# --- Stage 1: Build ---
# Use the official Temurin JDK 21 image for building
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy the Gradle wrapper and build files
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# Copy the source code
COPY src ./src

# Build the application, skipping tests
RUN ./gradlew build -x test

# --- Stage 2: Runtime ---
# Use the minimal JRE image for the final container
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy only the built JAR file from the 'build' stage
COPY --from=build /app/build/libs/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]