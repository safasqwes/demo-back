ARG JAVA_VERSION=17

# Build stage
FROM maven:3.8.5-openjdk-${JAVA_VERSION} AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Skip tests to speed up build
RUN echo "检查Java maven版本:"
RUN java -version
RUN mvn -version
RUN mvn clean compile -e
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:${JAVA_VERSION}-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
