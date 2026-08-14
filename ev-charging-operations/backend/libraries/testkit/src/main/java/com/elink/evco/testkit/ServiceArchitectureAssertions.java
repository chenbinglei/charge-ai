package com.elink.evco.testkit;

import com.tngtech.archunit.core.importer.ClassFileImporter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * W0 共用架构检查：阻止传统分层出现反向或跨层直接依赖。
 */
public final class ServiceArchitectureAssertions {
    private ServiceArchitectureAssertions() {
    }

    public static void assertLayerBoundaries(String basePackage) {
        var classes = new ClassFileImporter().importPackages(basePackage);

        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAnyPackage("..mapper..", "..entity..")
                .check(classes);
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..vo..")
                .check(classes);
        noClasses().that().resideInAPackage("..mapper..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "..dto..", "..vo..")
                .check(classes);
    }
}
