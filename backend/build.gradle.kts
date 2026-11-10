import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("net.ltgt.errorprone") version "4.1.0"
    id("com.github.spotbugs") version "6.0.18"
    id("com.google.cloud.tools.jib") version "3.4.4"   // container image, no Dockerfile
    id("org.cyclonedx.bom") version "3.0.0"            // CycloneDX SBOM (cyclonedxBom task)
    id("com.diffplug.spotless") version "7.0.2"        // auto-formatter (./gradlew spotlessApply)
    checkstyle
    jacoco
}

group = "com.boilerplate"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

// Optional service scaffolds inherit the same Java version and repository setup.
subprojects {
    group = rootProject.group
    version = rootProject.version
    repositories { mavenCentral() }
    apply(plugin = "java")
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }
}

val serviceNamespaces = mapOf(
    ":services:identity-service" to "identity",
    ":services:customer-service" to "customers",
    ":services:catalog-service" to "catalog",
    ":services:order-service" to "orders",
    ":services:gateway-service" to "gateway",
)

subprojects {
    val serviceNamespace = serviceNamespaces[path] ?: return@subprojects

    dependencies.add("testImplementation", "com.tngtech.archunit:archunit-junit5:1.3.0")
    sourceSets.named("test") {
        java.srcDir(rootProject.file("gradle/architecture-tests/src/test/java"))
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("boilerplate.architecture.service", serviceNamespace)
    }
}

tasks.register("buildServices") {
    group = "build"
    description = "Builds the optional extracted-service scaffolds."
    dependsOn(subprojects.map { it.tasks.named("build") })
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("org.springframework.boot:spring-boot-jackson2") // temporary compatibility for existing Jackson 2 integrations
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ── Observability ──────────────────────────────────────────────────────────
    // Prometheus scrape endpoint at /actuator/prometheus.
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    // Distributed tracing: trace/span ids in every log line + OTLP export (opt-in via
    // management.tracing.sampling.probability — defaults to 0, see application.yml).
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Cluster-safe scheduling: only ONE instance runs each housekeeping job (not the
    // outbox relay — that one is parallel-safe via FOR UPDATE SKIP LOCKED). See SchedulingConfig.
    implementation("net.javacrumbs.shedlock:shedlock-spring:7.10.1")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:7.10.1")

    // ── Resilience for outbound calls ──────────────────────────────────────────
    // Circuit breaker / retry / bulkhead as annotations; metrics auto-exported to Micrometer.
    // Reference wiring: infrastructure.payments.ResilientPaymentGateway.
    implementation("io.github.resilience4j:resilience4j-spring-boot4:2.4.0")
    implementation("org.springframework.boot:spring-boot-starter-aspectj") // proxies the resilience4j annotations

    // ── Caching ────────────────────────────────────────────────────────────────
    // Spring Cache abstraction + Caffeine (in-process). For multi-instance, swap the
    // CacheManager for RedisCacheManager under a profile — see config.CacheConfig / ARCHITECTURE.md.
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Optional: only wired under the `kafka` profile (see infrastructure.messaging.KafkaConfig).
    // Its autoconfiguration is excluded in application.yml so the default path stays broker-free.
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

    // Argon2id password hashing (required by Spring Security's Argon2PasswordEncoder)
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:1.21.3")
    testImplementation("org.testcontainers:postgresql:1.21.3")
    testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")

    // Error Prone — compile-time bug checker (closest to Clippy)
    errorprone("com.google.errorprone:error_prone_core:2.36.0")
    // Keep the compiler tooling's transitive Guava on its patched baseline too.
    errorprone("com.google.guava:guava:33.7.2-jre")
    // NullAway on top of Error Prone — enforces @NonNull discipline
    errorprone("com.uber.nullaway:nullaway:0.12.3")
}

// Boot 4.1.2 fixes the critical Spring/Tomcat and Jackson advisories. These
// remaining transitive libraries are not managed at patched versions by its BOM.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        when (requested.group to requested.name) {
            "at.yawk.lz4" to "lz4-java" -> useVersion("1.11.4")
            "commons-beanutils" to "commons-beanutils" -> useVersion("1.11.0")
            "com.google.guava" to "guava" -> useVersion("33.7.2-jre")
            "io.opentelemetry" to requested.name -> useVersion("1.67.0")
            "org.apache.httpcomponents.client5" to "httpclient5" -> useVersion("5.6.3")
            "org.apache.httpcomponents.core5" to "httpcore5",
            "org.apache.httpcomponents.core5" to "httpcore5-h2" -> useVersion("5.4.3")
            "org.codehaus.plexus" to "plexus-utils" -> useVersion("3.6.1")
            "org.xerial.snappy" to "snappy-java" -> useVersion("1.1.10.9")
        }
    }
}

// ── Error Prone ────────────────────────────────────────────────────────────────
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xmaxerrs")
    options.compilerArgs.add("500")
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        disable(
            "MissingSummary",    // Javadoc not required
            "InlineMeSuggester", // Not relevant for services
            "UnusedMethod",      // Spring beans often called via reflection
            "JavaUtilDate",      // JJWT library forces java.util.Date usage
        )
        // NullAway: great goal but needs @Nullable annotations codebase-wide first
        check("NullAway", net.ltgt.gradle.errorprone.CheckSeverity.WARN)
        option("NullAway:AnnotatedPackages", "com.boilerplate")
        // Spring @Value fields are injected after construction — not uninitialized
        option("NullAway:CustomInitializerAnnotations", "org.springframework.beans.factory.annotation.Value")
    }
}

// Error Prone / NullAway add little on test code and clash badly with Mockito
// (@Mock / @InjectMocks fields look "uninitialized"). Style is still enforced by Checkstyle.
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.isEnabled.set(false)
}

// ── SpotBugs ───────────────────────────────────────────────────────────────────
spotbugs {
    toolVersion.set("4.8.6")
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html") { required.set(true) }
    reports.create("xml")  { required.set(false) }
}

// ── Checkstyle ─────────────────────────────────────────────────────────────────
checkstyle {
    toolVersion = "10.21.4"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

// ── Spotless — formatter (Checkstyle reports, Spotless fixes) ────────────────────
// palantir-java-format: 4-space indent, 120 columns, K&R braces — matches checkstyle.xml.
// `./gradlew spotlessApply` to format; `spotlessCheck` is wired into `check` by the plugin.
spotless {
    java {
        target("src/**/*.java")
        removeUnusedImports()
        palantirJavaFormat("2.50.0")
        trimTrailingWhitespace()
        endWithNewline()
    }
}


// ── Tests & coverage ───────────────────────────────────────────────────────────
tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        // A floor that catches "half the tests got deleted", not a target. Ratchet it up as
        // the suite grows; never lower it to make a build pass. The HTML report
        // (build/reports/jacoco/test/html) is where you read the real number.
        rule {
            limit {
                counter = "INSTRUCTION"
                minimum = "0.50".toBigDecimal() // suite is at ~60%; this is the "don't regress badly" line
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// ── Container image (Jib — the canonical build; the compose Dockerfile is for local use) ──
jib {
    from {
        // Multi-arch base. Pin by digest for a real deployment; the tag keeps the boilerplate
        // current and Trivy/Renovate flag it when it drifts.
        image = "eclipse-temurin:21-jre"
        platforms {
            platform { architecture = "amd64"; os = "linux" }
            platform { architecture = "arm64"; os = "linux" }
        }
    }
    to {
        image = System.getenv("IMAGE") ?: "boilerplate-spring-backend"
        tags = setOf("latest", version.toString())
    }
    container {
        ports = listOf("8080")
        user = "1000:1000" // non-root, numeric so it needs no /etc/passwd entry (k8s runAsNonRoot)
        jvmFlags = listOf(
            "-XX:MaxRAMPercentage=75.0",
            "-XX:+ExitOnOutOfMemoryError",
            "-Djava.security.egd=file:/dev/./urandom",
        )
        environment = mapOf("SPRING_PROFILES_ACTIVE" to (System.getenv("SPRING_PROFILES_ACTIVE") ?: "prod"))
        labels = mapOf(
            "org.opencontainers.image.title" to "boilerplate-spring backend",
            "org.opencontainers.image.source" to (System.getenv("IMAGE_SOURCE") ?: ""),
            "org.opencontainers.image.version" to version.toString(),
        )
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}

// cyclonedxBom (defaults): writes build/reports/bom.json + bom.xml over runtimeClasspath.
