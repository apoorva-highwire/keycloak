# Adding swagger-codegen, springdoc (Swagger UI), and Lombok to `content-sync`

Three separate additions, despite all three touching "API"/"models" — they don't depend on each
other:

1. **swagger-codegen** — generates DTOs + Bean Validation annotations *from* an OpenAPI spec file
   you provide. Input: a spec. Output: Java model classes.
2. **springdoc-openapi (Swagger UI)** — the opposite direction: generates an interactive API doc
   page *from* your own `@RestController` classes, same as `iam-service` already has at
   `/swagger-ui/index.html`. No spec file involved.
3. **Lombok** — boilerplate reduction (getters/setters/builders) for code you write by hand. Not
   related to the generated DTOs — see the honesty note in §1.

---

## 1. swagger-codegen — generate DTOs + validators from a spec

### The real mechanism

`swagger-codegen` ([swagger-api/swagger-codegen](https://github.com/swagger-api/swagger-codegen))
has **no official Gradle plugin** — only a CLI jar and a Maven plugin. The standard way to drive it
from Gradle is the community plugin `org.hidetake.swagger.generator`
([int128/gradle-swagger-generator-plugin](https://github.com/int128/gradle-swagger-generator-plugin)),
which wraps the real `swagger-codegen-cli.jar` as a Gradle task. Confirmed from its own README —
this is the real DSL, not a guess:

### `build.gradle.kts`

```kotlin
plugins {
    id("org.hidetake.swagger.generator") version "2.19.2"
}

dependencies {
    // The real swagger-codegen CLI this plugin wraps — pin a version explicitly.
    swaggerCodegen("io.swagger:swagger-codegen-cli:2.4.34")
}

swaggerSources {
    create("contentSync") {
        // Placeholder path — point this at the real spec once you have one.
        // Not drafted here: this doc documents the generator setup only.
        inputFile = file("src/main/resources/openapi/content-sync.yaml")
        code {
            language = "spring"                      // generator target — spring-mvc-style Java models + API interfaces
            configFile = file("codegen-config.json")
        }
    }
}

// Generated sources land under build/swagger-code-contentSync/src/main/java by default —
// add that to the main source set so compileJava actually picks it up.
sourceSets {
    main {
        java {
            srcDir("$buildDir/swagger-code-contentSync/src/main/java")
        }
    }
}

tasks.named("compileJava") {
    dependsOn("generateSwaggerCode")
}
```

### `codegen-config.json`

```json
{
  "library": "spring-mvc",
  "modelPackage": "org.highwire.contentsync.generated.model",
  "apiPackage": "org.highwire.contentsync.generated.api",
  "invokerPackage": "org.highwire.contentsync.generated"
}
```

### Bean Validation annotations on the generated models

Rather than guessing a flag name here, find the real, current option list for whichever language
template you target — swagger-codegen's own documented way to do this:

```bash
java -jar ~/.gradle/caches/modules-2/.../swagger-codegen-cli-2.4.34.jar config-help -l spring
```

(or download the CLI jar directly from Maven Central and run it standalone). This prints every
config key that generator version actually supports, including whatever controls Bean Validation
(`@NotNull`/`@Size`/etc. derived from the spec's own `required`/`minLength`/etc.) — add the real key
it reports to `codegen-config.json` above, rather than copying a key name from somewhere else that
may not match this generator/version.

### Honesty note: Lombok is NOT a verified swagger-codegen output option

I could not confirm swagger-codegen's `spring`/`java` templates have a dedicated "emit
Lombok-annotated models" config key (this is more reliably true of OpenAPI Generator, the actively
maintained fork, not confirmed here for swagger-codegen itself). Generated DTOs will have plain
getters/setters/`equals`/`hashCode`/`toString` from the template, not Lombok annotations. If you
want Lombok specifically on the *generated* models, verify via the same `config-help -l spring`
command above rather than assuming it exists — don't configure a Lombok-related key you haven't
seen in that real option list.

### Run it

```bash
./gradlew generateSwaggerCode
```
Verify by actually checking the output directory exists and contains `.java` files — don't assume
success from exit code alone on a first run, since a misconfigured `inputFile`/`language` can
silently produce an empty output directory on some generator versions:
```bash
find build/swagger-code-contentSync -name "*.java"
```

---

## 2. springdoc-openapi — Swagger UI for `content-sync`'s own endpoints

Same dependency `iam-service` uses — confirmed in `iam/pom.xml` (there, pulled in as part of the
existing Maven setup; here, declared directly):

```kotlin
dependencies {
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
}
```

No further config needed for a minimal setup — it scans your `@RestController` classes
automatically. Two endpoints come up once the app runs, same paths as `iam-service`:
- `/v3/api-docs` — raw OpenAPI JSON generated from your actual controllers
- `/swagger-ui/index.html` — the interactive UI

**Don't forget the `SecurityConfig` permit rule** — `iam-service`'s own `SecurityConfig` had to add
`/swagger-ui/**` and `/v3/api-docs/**` to `permitAll()`; without it, these 401 along with everything
else once `auth-sdk`'s JWT filter is wired in (see `GRADLE-SPRING-BOOT4-JAVA25-SETUP.md` §5).

---

## 3. Lombok — for your own hand-written code

Standard Gradle setup, nothing project-specific:

```kotlin
dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    testCompileOnly("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
}
```

Use it on code you write by hand (service classes, mappers between the generated DTOs and your own
domain types, etc.) — not on the swagger-codegen output itself per the §1 honesty note.

---

## 4. Verify, in order, before trusting this is done

1. `./gradlew generateSwaggerCode` — confirm real `.java` files land in the output dir (§1).
2. `./gradlew compileJava` — confirms the generated sources + Lombok annotation processing +
   springdoc dependency all compile together, not just each in isolation.
3. `./gradlew bootRun`, then hit `/swagger-ui/index.html` — confirms springdoc actually serves a
   page, and that `SecurityConfig`'s `permitAll()` rule (§2) is in place.
4. Nothing in this doc has been run yet this session — unlike `GRADLE-SPRING-BOOT4-JAVA25-SETUP.md`,
   none of steps 1–3 above are verified live. Treat the exact plugin/dependency versions above as
   current-as-of-research, not confirmed-by-running, until you've actually executed them here.
