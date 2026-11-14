# syntax=docker/dockerfile:1

# Official java image
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

# build directory INSIDE the container
WORKDIR /builder

# Layer caching: copy only pom.xml first so dependency resolution can be cached
# independently of source changes.
COPY pom.xml .

# BuildKit cache mount keeps the local Maven repository between builds, so this
# becomes a no-op after the first build instead of re-downloading every time.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline

# Copy source code and build it. Tests run in CI (see .github/workflows), not in
# the image build; -Dmaven.test.skip also skips compiling test sources.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -Dmaven.test.skip=true package

# create runtime image:
FROM eclipse-temurin:25-jre-alpine
WORKDIR /springapp

COPY --from=builder /builder/target/*.jar springapp.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "springapp.jar"]
