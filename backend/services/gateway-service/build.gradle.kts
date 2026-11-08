plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.9")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.4")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
