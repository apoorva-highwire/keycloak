# Custom Keycloak image with HighWire's base + per-publisher login themes,
# and the highwire-keycloak-mapper SPI provider, baked in.
# Build context is the keycloak/ root (docker-compose.yml sets context: ..),
# so `themes/`/`providers/` below resolve relative to this file. For local
# dev, docker-compose also bind-mounts themes/ over this so CSS/FTL edits
# don't require a rebuild — the provider jar isn't bind-mounted, since a
# Java SPI provider needs `kc.sh build` to be re-registered anyway, so
# there's no edit-without-rebuild path for it like there is for themes.

# Builds the provider jar with the project's own Maven, not one baked into
# the Keycloak image — keeps the Keycloak image itself JDK-version-agnostic
# of whatever this provider happens to compile against.
FROM maven:3.9-eclipse-temurin-21 AS provider-builder
WORKDIR /build
COPY providers/highwire-keycloak-mapper/pom.xml .
COPY providers/highwire-keycloak-mapper/src ./src
RUN mvn -q -B package -DskipTests

FROM quay.io/keycloak/keycloak:26.8.0 AS builder

COPY themes/ /opt/keycloak/themes/
COPY --from=provider-builder /build/target/highwire-keycloak-mapper-*.jar /opt/keycloak/providers/

# Pre-compile the server config (providers, themes) into a faster-starting build.
RUN /opt/keycloak/bin/kc.sh build --db=postgres

FROM quay.io/keycloak/keycloak:26.8.0
COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
