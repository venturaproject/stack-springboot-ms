package com.boilerplate.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Executable version of ARCHITECTURE.md. These rules are what actually stop the layering
 * from drifting — keep them green.
 */
@AnalyzeClasses(packages = "com.boilerplate", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain")
            .definedBy("com.boilerplate.domain..")
            .layer("Application")
            .definedBy("com.boilerplate.application..")
            .layer("Infrastructure")
            .definedBy("com.boilerplate.infrastructure..")
            .layer("Config")
            .definedBy("com.boilerplate.config..")
            .whereLayer("Infrastructure")
            .mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Infrastructure", "Config");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_application_or_infrastructure = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule domain_uses_only_spring_data_from_spring = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat(resideInAPackage("org.springframework..")
                    .and(com.tngtech.archunit.base.DescribedPredicate.not(
                            resideInAPackage("org.springframework.data.."))))
            .because("the domain may use Spring Data abstractions (the persistence-port "
                    + "vocabulary: Repository marker, @DomainEvents) but nothing else Spring");

    @ArchTest
    static final ArchRule domain_repositories_are_not_jpa_repositories = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .beAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
            .because("domain ports expose only what the domain needs, not the full JpaRepository surface");

    @ArchTest
    static final ArchRule application_is_free_of_web_and_infrastructure = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..infrastructure..",
                    "org.springframework.web..",
                    "jakarta.servlet..",
                    "org.hibernate..",
                    "org.springframework.security..")
            .because("use cases orchestrate the domain; framework details belong to infrastructure");

    @ArchTest
    static final ArchRule controllers_live_only_in_the_http_package = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage("..infrastructure.http.controllers..");

    @ArchTest
    static final ArchRule entities_have_no_setters = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(Entity.class)
            .should()
            .haveNameMatching("set[A-Z].*")
            .because("aggregates protect their invariants through intention-revealing methods");

    @ArchTest
    static final ArchRule no_domain_entity_is_returned_from_a_controller = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("..infrastructure.http.controllers..")
            .and()
            .arePublic()
            .should()
            .haveRawReturnType(resideInAPackage("com.boilerplate.domain.."))
            .because("controllers return response DTOs, never aggregates");
}
