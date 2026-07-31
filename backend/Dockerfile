# Official java image
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

# build directory INSIDE the container
WORKDIR /builder

# Layer caching: Copy only pom.xml and fetch dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build that too
COPY src ./src
RUN mvn clean package -DskipTests && \
    mv target/*.jar target/springapp.jar

# create runtime image:
FROM eclipse-temurin:25-jre-alpine
WORKDIR /springapp

COPY --from=builder /builder/target/*.jar springapp.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "springapp.jar"]