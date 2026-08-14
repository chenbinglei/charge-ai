package com.elink.evco.testkit;

import com.tngtech.archunit.core.importer.ClassFileImporter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Shared W0 checks for the fixed service-layer dependency direction.
 */
public final class ServiceArchitectureAssertions {
    private ServiceArchitectureAssertions() {
    }

    public static void assertLayerBoundaries(String basePackage) {
        var classes = new ClassFileImporter().importPackages(basePackage);

        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..interfaces..", "..infrastructure..")
                .check(classes);
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..interfaces..")
                .check(classes);
    }
}
