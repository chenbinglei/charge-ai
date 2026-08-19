package com.elink.evco.platform.iam;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * IAM 模块架构守护：接口规范约定 Controller 只编排 DTO/VO 并调用 Service，
 * 不得直连 Mapper 或把实体泄漏到 Web 层；Service 不得反向依赖 Controller。
 */
class IamArchitectureTest {

    /** 导入平台服务全部字节码；每次运行读取当前编译产物。 */
    private final JavaClasses classes =
            new ClassFileImporter().importPackages("com.elink.evco.platform");

    /** 规则：Controller 不得依赖 Mapper。 */
    private final ArchRule controllersMustNotUseMappers =
            noClasses().that().resideInAPackage("..iam.controller..")
                    .should().dependOnClassesThat().resideInAPackage("..iam.mapper..");

    /** 规则：Controller 不得依赖实体（Web 层只出现 DTO/VO）。 */
    private final ArchRule controllersMustNotUseEntities =
            noClasses().that().resideInAPackage("..iam.controller..")
                    .should().dependOnClassesThat().resideInAPackage("..iam.entity..");

    /** 规则：Service 不得依赖 Controller（依赖方向单向）。 */
    private final ArchRule servicesMustNotUseControllers =
            noClasses().that().resideInAPackage("..iam.service..")
                    .should().dependOnClassesThat().resideInAPackage("..iam.controller..");

    /** 验证 Controller 与 Mapper 层隔离。 */
    @Test
    @DisplayName("架构：Controller 不得直连 Mapper")
    void controllersMustNotDependOnMappers() {
        controllersMustNotUseMappers.check(classes);
    }

    /** 验证实体不出现在 Web 层。 */
    @Test
    @DisplayName("架构：Controller 不得引用实体类")
    void controllersMustNotDependOnEntities() {
        controllersMustNotUseEntities.check(classes);
    }

    /** 验证分层依赖方向。 */
    @Test
    @DisplayName("架构：Service 不得依赖 Controller")
    void servicesMustNotDependOnControllers() {
        servicesMustNotUseControllers.check(classes);
    }
}
