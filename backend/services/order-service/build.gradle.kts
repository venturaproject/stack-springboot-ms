plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.2") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
