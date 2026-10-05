# Build context is keycloak/ (see docker/docker-compose.yml) so this stage
# can reach ../auth-sdk too — iam-service depends on org.highwire:auth-sdk,
# which only exists in whichever Maven repo this build populates; it's
# installed here first, same as iam-service itself.
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY auth-sdk ./auth-sdk
RUN mvn -q -B -f auth-sdk/pom.xml install -DskipTests

COPY iam/pom.xml ./iam/pom.xml
COPY iam/src ./iam/src
RUN mvn -q -B -f iam/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/iam/target/iam-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
