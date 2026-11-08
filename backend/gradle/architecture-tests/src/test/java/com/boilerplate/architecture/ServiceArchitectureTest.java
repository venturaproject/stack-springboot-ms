package com.boilerplate.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Objects;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

/** Architecture guardrails shared by every independently-built service module. */
@AnalyzeClasses(packages = "com.boilerplate", importOptions = ImportOption.DoNotIncludeTests.class)
class ServiceArchitectureTest {

    private static final String SERVICE = Objects.requireNonNull(
            System.getProperty("boilerplate.architecture.service"),
            "Gradle must set boilerplate.architecture.service for each service module");
    private static final String SERVICE_PACKAGE = "com.boilerplate." + SERVICE;
    private static final String DOMAIN_PACKAGE = SERVICE_PACKAGE + ".domain..";
    private static final String APPLICATION_PACKAGE = SERVICE_PACKAGE + ".application..";
    private static final String INFRASTRUCTURE_PACKAGE = SERVICE_PACKAGE + ".infrastructure..";
    private static final String HTTP_ADAPTER_PACKAGE = SERVICE_PACKAGE + ".infrastructure.http..";
    private static final String CONFIG_PACKAGE = SERVICE_PACKAGE + ".config..";
    private static final String[] OTHER_SERVICE_PACKAGES = {
        "com.boilerplate.identity..",
        "com.boilerplate.customers..",
        "com.boilerplate.catalog..",
        "com.boilerplate.orders..",
        "com.boilerplate.gateway.."
    };

    @ArchTest
    static final ArchRule production_classes_stay_in_their_service_namespace = classes()
            .that()
            .resideInAPackage("com.boilerplate..")
            .should()
            .resideInAPackage(SERVICE_PACKAGE + "..");

    @ArchTest
    static final ArchRule services_do_not_depend_on_other_service_namespaces = noClasses()
            .that()
            .resideInAPackage(SERVICE_PACKAGE + "..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(otherServicePackages())
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule each_service_declares_its_own_boot_application = classes()
            .that()
            .areAnnotatedWith(SpringBootApplication.class)
            .should()
            .resideInAPackage(SERVICE_PACKAGE)
            .andShould()
            .haveSimpleNameEndingWith("Application");

    @ArchTest
    static final ArchRule layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain")
            .definedBy(DOMAIN_PACKAGE)
            .layer("Application")
            .definedBy(APPLICATION_PACKAGE)
            .layer("Infrastructure")
            .definedBy(INFRASTRUCTURE_PACKAGE)
            .layer("Config")
            .definedBy(CONFIG_PACKAGE)
            .whereLayer("Infrastructure")
            .mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Infrastructure", "Config")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Config")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_does_not_depend_on_application_or_infrastructure = noClasses()
            .that()
            .resideInAPackage(DOMAIN_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(APPLICATION_PACKAGE, INFRASTRUCTURE_PACKAGE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_does_not_depend_on_web_or_infrastructure = noClasses()
            .that()
            .resideInAPackage(APPLICATION_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    INFRASTRUCTURE_PACKAGE,
                    "org.springframework.web..",
                    "jakarta.servlet..",
                    "org.hibernate..",
                    "org.springframework.security..")
            .because("use cases depend on inward ports, not transport or persistence adapters")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_does_not_depend_on_web_or_security_frameworks = noClasses()
            .that()
            .resideInAPackage(DOMAIN_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework.web..",
                    "org.springframework.security..",
                    "jakarta.servlet..",
                    "org.hibernate..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers_live_in_the_http_adapter = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage(HTTP_ADAPTER_PACKAGE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistence_entities_have_no_setters = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith("jakarta.persistence.Entity")
            .should()
            .haveNameMatching("set[A-Z].*")
            .because("aggregate state changes must preserve domain invariants")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers_do_not_return_domain_entities = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage(HTTP_ADAPTER_PACKAGE)
            .and()
            .arePublic()
            .should()
            .haveRawReturnType(
                    com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage(DOMAIN_PACKAGE))
            .allowEmptyShould(true);

    @ArchTest
    static void imported_code_is_confined_to_the_module(JavaClasses importedClasses) {
        // Forces ArchUnit to inspect this module's production classes at test time. The
        // package-containment rule above then rejects leaked classes from sibling modules.
        if (importedClasses.isEmpty()) {
            throw new AssertionError("Expected production classes under " + SERVICE_PACKAGE);
        }
    }

    private static String[] otherServicePackages() {
        return java.util.Arrays.stream(OTHER_SERVICE_PACKAGES)
                .filter(packagePattern -> !packagePattern.startsWith(SERVICE_PACKAGE + "."))
                .toArray(String[]::new);
    }
}
