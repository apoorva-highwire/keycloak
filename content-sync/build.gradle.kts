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
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // spring-security-config / -oauth2-resource-server / -oauth2-jose / -core all arrive
    // transitively via auth-sdk's own pom.xml — do not re-declare them unless you need a
    // newer/older version than auth-sdk pulls in.

    // The generated model (IngestRequest) targets an older stack than the rest of this project —
    // confirmed live via real compile errors, not guessed: io.swagger.annotations (Swagger 1.x,
    // not OpenAPI 3's io.swagger.v3.oas.annotations), javax.validation (not jakarta.validation),
    // and javax.annotation.Generated. These coexist on the classpath fine (different packages
    // than jakarta.validation), but see the IMPORTANT note in IngestController.java: Spring Boot
    // 4's Hibernate Validator processes jakarta.validation.* annotations, NOT these javax ones —
    // @Valid on this generated DTO will NOT actually enforce @NotNull/@Size/@Pattern at runtime.
    compileOnly("io.swagger:swagger-annotations:1.6.14")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6") // version matches the generator's own generated pom.xml
    implementation("javax.validation:validation-api:2.0.1.Final")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// swagger-codegen has no official Gradle plugin, and the one community wrapper
// (org.hidetake.swagger.generator) is broken against Gradle 9.1.0 — confirmed live: it calls
// Project.javaexec(Closure), a Groovy convenience method Gradle removed entirely by this
// version. Driving the real swagger-codegen-cli.jar directly via a JavaExec task instead — the
// modern, supported way to do the same thing, with no plugin-compatibility risk.
val swaggerCodegen: Configuration by configurations.creating

dependencies {
    // io.swagger:swagger-codegen-cli (the "v2" line) only parses Swagger 2.0 specs — confirmed
    // live it fails on an `openapi: 3.0.3` document. io.swagger.codegen.v3 is the correct
    // artifact for OpenAPI 3 specs, version confirmed current via Maven Central search.
    swaggerCodegen("io.swagger.codegen.v3:swagger-codegen-cli:3.0.68")
}

val generatedSwaggerDir = layout.buildDirectory.dir("generated/swagger-code")

val generateSwaggerCode by tasks.registering(JavaExec::class) {
    group = "codegen"
    description = "Generates Java DTOs + API interfaces from src/main/resources/openapi/content-sync.yaml"

    inputs.file("src/main/resources/openapi/content-sync.yaml")
    inputs.file("codegen-config.json")
    outputs.dir(generatedSwaggerDir)

    classpath = swaggerCodegen
    mainClass.set("io.swagger.codegen.v3.cli.SwaggerCodegen") // confirmed by running it — BUILD SUCCESSFUL
    args = listOf(
        "generate",
        "-i", "src/main/resources/openapi/content-sync.yaml",
        "-l", "spring",
        "-c", "codegen-config.json",
        "-o", generatedSwaggerDir.get().asFile.path
    )

    doFirst {
        generatedSwaggerDir.get().asFile.mkdirs()
    }
}

// Generated sources land under <output>/src/main/java — add that to the main source set so
// compileJava actually picks it up. Excludes the generated API controller/interface (we write
// our own, by hand, wrapping ApiResponse — see IngestController/IngestService) and the generated
// standalone-app scaffold (WebApplication has its own main(), SwaggerUiConfiguration/
// WebMvcConfiguration/SwaggerDocumentationConfig would collide with our own SecurityConfig) —
// confirmed live these get generated alongside the model and would otherwise break compilation.
// io.swagger.configuration.NotUndefined/NotUndefinedValidator ARE kept — IngestRequest's
// @NotUndefined class-level annotation genuinely depends on them.
sourceSets {
    main {
        java {
            srcDir(generatedSwaggerDir.map { it.dir("src/main/java") })
            exclude("org/highwire/contentsync/generated/api/**")
            exclude("io/swagger/configuration/WebApplication.java")
            exclude("io/swagger/configuration/WebMvcConfiguration.java")
            exclude("io/swagger/configuration/SwaggerUiConfiguration.java")
            exclude("io/swagger/configuration/SwaggerDocumentationConfig.java")
            // Date/time (de)serializer support — not needed since IngestRequest has no date
            // fields, and they require com.fasterxml.jackson.datatype.threetenbp, which we don't
            // otherwise depend on. Confirmed live these fail to compile without it.
            exclude("io/swagger/configuration/RFC3339DateFormat.java")
            exclude("io/swagger/configuration/LocalDateConverter.java")
            exclude("io/swagger/configuration/LocalDateTimeConverter.java")
            exclude("io/swagger/configuration/CustomInstantDeserializer.java")
        }
    }
}

tasks.named("compileJava") {
    dependsOn(generateSwaggerCode)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}