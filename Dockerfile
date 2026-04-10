# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies to cache them
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
