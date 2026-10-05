# Setting up a Gradle + Spring Boot 4 + Java 25 service that uses `auth-sdk`

How to stand up a **new** service — Gradle build, Java 25 toolchain, Spring Boot 4 — that consumes
`org.highwire:auth-sdk` from **GitHub Packages**, the way `content-sync` does. `auth-sdk` itself is
still built with **Maven** and targets **Java 21**; none of that matters to a Gradle consumer once
the jar is published. Everything below is grounded in `auth-sdk`'s and `iam-service`'s actual
`pom.xml`/source — nothing here is a generic Gradle tutorial.

**Supersedes the local-`mavenLocal()` version of this doc** — `auth-sdk` is now a real published
package (`org.highwire:auth-sdk:1.0.1`, repo `github.com/highwire/auth-sdk`, publish pipeline in its
own `.github/workflows/publish.yml`), not something you build from source yourself.

---

## 1. Prerequisite: a GitHub PAT with `read:packages`

GitHub Packages requires authentication for **reads**, even on a public repo — not just for
publishing. Before touching Gradle:

1. Create a **classic** PAT at **https://github.com/settings/tokens/new** (fine-grained PATs have had
   inconsistent GitHub Packages support — classic is the reliable path).
2. Check **`read:packages`**. If `highwire/auth-sdk` is a **private** repo, also check **`repo`** —
   `read:packages` alone 403s against a private repo's packages, a real GitHub quirk, not a bug on
   your end.
3. Generate it, copy it immediately (GitHub won't show it again).
4. Put it in **`~/.gradle/gradle.properties`** — your home directory, never inside the project:
   ```properties
   gpr.user=YOUR_ACTUAL_GITHUB_USERNAME
   gpr.token=ghp_xxxxxxxxxxxxxxxxxxxx
   ```
   **Verified live**: GitHub Packages' Maven registry only checks the token over HTTP Basic auth —
   `gpr.user` can be any non-empty string, including a literal placeholder, and resolution still
   succeeds (confirmed: `./gradlew dependencies` resolved `org.highwire:auth-sdk:1.0.1` fine with
   `gpr.user` left as unedited placeholder text). Still use your real username for clarity/auditing —
   just don't expect a wrong one to be *why* something fails if you hit an auth error; look at the
   token instead.

---

## 2. Project layout

```
keycloak/
  auth-sdk/              # existing, Maven, publishes to GitHub Packages via its own CI
  iam/                   # existing, Maven
  content-sync/          # NEW
    settings.gradle.kts
    build.gradle.kts
    gradle/wrapper/...
    src/main/java/...
    src/main/resources/application.yml
    Dockerfile
```

Keep it alongside `iam/`, not nested inside it — same sibling relationship `iam/` and `admin-ui/`
already have under `keycloak/`.

---

## 3. `settings.gradle.kts`

```kotlin
rootProject.name = "content-sync"
```

## 4. `build.gradle.kts`

```kotlin
plugins {
    java
    id("org.springframework.boot") version "4.1.1"       // match auth-sdk's spring-boot.version exactly
    id("io.spring.dependency-management") version "1.1.7" // check latest compatible release before pinning
}

group = "org.highwire"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/highwire/auth-sdk")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
}

dependencyManagement {
    imports {
        // Same BOM auth-sdk's pom.xml imports — pinning both to 4.1.1 keeps every transitive
        // (Spring Security, Jackson, etc.) at one resolved version instead of two builds silently
        // picking different patch versions of the same library.
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.1")
    }
}

dependencies {
    implementation("org.highwire:auth-sdk:1.0.1")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // spring-security-config / -oauth2-resource-server / -oauth2-jose / -core all arrive
    // transitively via auth-sdk's own pom.xml — do not re-declare them unless you need a
    // newer/older version than auth-sdk pulls in.

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
```

**Why the GitHub Packages repository must be listed before `mavenCentral()`**: Gradle resolves
repositories in declaration order for a given dependency. `org.highwire:auth-sdk` exists nowhere but
that registry — list it first so Gradle checks there before giving up.

**Verify it actually resolves** before writing any application code — confirmed working end to end:
```bash
./gradlew dependencies --configuration compileClasspath > depcheck.txt 2>&1
grep "auth-sdk" depcheck.txt
```
Don't pipe straight through `grep`/`tail` on the live command — a long Spring Boot transitive tree
can push the line you want past a `tail -N` window (this happened once while verifying this doc:
`org.highwire:auth-sdk:1.0.1` is listed near the TOP of the tree as a direct dependency, and a
`| tail -60` on the full run discarded it along with everything else above the last 60 lines,
producing a false "it's missing" read on a build that had actually succeeded). Capture to a file
first, then search the file.
- **401** → `gpr.token` is wrong/expired/missing the right scope — the username field itself doesn't
  matter (see §1).
- **404** → the `v1.0.1` tag's publish workflow may not have completed — check
  `https://github.com/highwire/auth-sdk/actions`.

---

## 5. Minimal wiring to actually use `auth-sdk`

`auth-sdk` auto-configures `JwtDecoder`, `CallerResolvingFilter`, `RequestCorrelationFilter`,
`AccessLogFilter`, and `ControllerLoggingAspect` for you (`AuthSdkAutoConfiguration` /
`JwtAuthConfig`, confirmed in `auth-sdk/src/main/java/org/highwire/authsdk/config/`) — you don't
write any of that yourself. Two things your new service still owns:

### `application.yml` — the required `highwire.auth.*` properties

Confirmed against `AuthSdkProperties.java` — these are the exact keys, no others are required:

```yaml
highwire:
  auth:
    keycloak-url: http://keycloak:8080       # required, no default
    admin-realm: master                       # optional, defaults to "master"
    admin-client-id: content-sync             # required, no default
    admin-client-secret: ${KEYCLOAK_ADMIN_SECRET}  # required, no default — never commit this value
    # resource-server-client-id: ...          # optional — only if you'll do UMA2 permission checks
    # decision-cache-ttl-seconds: 30           # optional, defaults to 30
```

### `SecurityConfig.java` — your own, following `iam-service`'s exact pattern

`CallerResolvingFilter` is deliberately **not** auto-registered as a global filter (confirmed in
`AuthSdkAutoConfiguration`'s own javadoc — no numeric filter order reliably lands it after Spring
Security's chain without the full `spring-boot-starter-security` umbrella). Wire it the same way
`iam-service/src/main/java/org/highwire/iam/config/SecurityConfig.java` does:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http, JwtDecoder jwtDecoder, CallerResolvingFilter callerResolvingFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))
            .addFilterAfter(callerResolvingFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
```

If you also want the `ApiResponse`-shaped 401 body `iam-service` has (instead of Spring Security's
default), port `JsonAuthenticationEntryPoint` over — it's ~40 lines and has no `iam`-specific
dependency beyond its own `ApiResponse`/`ApiError` records, which you'd define identically in your
new service.

---

## 6. `Dockerfile` — Gradle build stage, Java 25 runtime, GitHub Packages auth

Single-stage Gradle build now (no more Maven stage to install `auth-sdk` from source — it's a real
published dependency), but the build container needs the same PAT credentials to resolve it:

```dockerfile
FROM gradle:9.1.0-jdk25 AS builder
WORKDIR /build
COPY . .
# GITHUB_ACTOR/GITHUB_TOKEN are read by build.gradle.kts's repository credentials block (see §4) —
# passed as build secrets, never baked into an image layer as a plain ARG/ENV would be.
RUN --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    GITHUB_ACTOR=$(cat /run/secrets/github_actor) \
    GITHUB_TOKEN=$(cat /run/secrets/github_token) \
    gradle build -x test --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build it with:
```bash
docker build \
  --secret id=github_actor,env=GITHUB_ACTOR \
  --secret id=github_token,env=GITHUB_TOKEN \
  -t content-sync .
```
(requires BuildKit, the default for current Docker — `DOCKER_BUILDKIT=1` if it isn't already)

**Verify before relying on this**: confirm `eclipse-temurin:25-jre` and `gradle:8-jdk25` actually
exist as published tags on Docker Hub at build time — don't assume the tag name from this doc alone;
`docker pull` each one once to confirm, the same way every other image tag in this project's
`docker-compose.yml` files was confirmed live rather than guessed.

---

## 7. Things verified live, and what's still open

**Verified, this setup, this session** — not asserted from docs alone:
- `brew`/`choco`/`scoop` were NOT available on the dev machine used here; Gradle was installed
  manually (download the `-bin.zip` from `services.gradle.org/distributions/`, extract, add `bin/` to
  `PATH`) — if you hit the same, that's the fallback, not a sign something's wrong.
- **Gradle 9.1.0 is the version to use** — confirmed via its own release notes: full Java 25 support,
  including as the *daemon* JVM, not just a toolchain target (earlier 8.x lines predate this).
  `gradle -v` after install should show `Gradle 9.1.0`.
- `gradle wrapper --gradle-version 9.1.0` run successfully inside `content-sync/`, producing
  `gradlew`/`gradlew.bat`/`gradle/wrapper/gradle-wrapper.properties`.
- `./gradlew dependencies --configuration compileClasspath` → **`BUILD SUCCESSFUL`**, with
  `org.highwire:auth-sdk:1.0.1` genuinely present in the resolved tree (see the grep caveat above) —
  the whole GitHub Packages chain (PAT → `gradle.properties` → `build.gradle.kts` repository block →
  resolution) works end to end as of this writing.

**Still open / not yet verified:**
- Spring Boot 4.1.1 actually *running* (not just resolving dependencies) on a Java 25 daemon/toolchain
  — only `dependencies` has been run so far, not `bootRun` or an actual started application.
- `gradle:9.1.0-jdk25`-style Docker image tags for §6 — not confirmed to exist; the dependency
  resolution above was run with a manually-installed local Gradle, not inside Docker.
- `SecurityConfig`'s filter ordering and the `JsonAuthenticationEntryPoint` port-over — neither has
  been written for `content-sync` yet, only the build file.
- **Spring Boot 4.1.1 on Java 25.** `iam-service` runs Spring Boot 4.1.1 on Java 21 today (confirmed
  live in its own startup logs — `Spring v7.0.9`). Java 25 is newer than what's been run in this
  project so far; nothing in Spring Boot 4's baseline (Java 17+) should reject it, but run
  `gradle bootRun` locally and confirm a clean startup before assuming parity.
- **`io.spring.dependency-management` plugin version** — pin whatever the latest release is at setup
  time; the `1.1.7` above is a placeholder, not a verified-current number.
- **`SecurityConfig`'s filter ordering** — `iam-service`'s own comments flag that this is fragile
  without the full Spring Security starter; after wiring yours, hit an endpoint with no token and
  confirm you get a real 401 (not a 403, not a silent pass-through) before moving on.
