# Single-stage Gradle build — auth-sdk is a real published dependency (GitHub Packages), not
# something built from source here, unlike iam-service's Maven-based Dockerfile.
#
# gradle:9.1.0-jdk25 tag NOT yet confirmed to exist on Docker Hub — verify with
# `docker pull gradle:9.1.0-jdk25` before relying on this; Gradle 9.1.0 + Java 25 was verified
# locally this session (manual install), not yet inside this container.
FROM gradle:9.1.0-jdk25 AS builder
WORKDIR /build
COPY . .
# GITHUB_ACTOR/GITHUB_TOKEN are read by build.gradle.kts's repository credentials block — passed
# as build secrets, never baked into an image layer as a plain ARG/ENV would be.
RUN --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    GITHUB_ACTOR=$(cat /run/secrets/github_actor) \
    GITHUB_TOKEN=$(cat /run/secrets/github_token) \
    gradle build -x test --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
