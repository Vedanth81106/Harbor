# Official java image
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

# build directory INSIDE the container
WORKDIR /builder

# copy the source code and build the jar
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# create runtime image:
FROM eclipse-temurin:25-jre-alpine
WORKDIR /springapp

COPY --from=builder /builder/target/*.jar springapp.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "springapp.jar"]