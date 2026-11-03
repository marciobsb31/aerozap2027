package br.com.aerozap.platformcore;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

/** Fronteiras de módulo (AGENTS.md): este serviço não conhece outros serviços nem acessa banco pela camada web. */
@AnalyzeClasses(packages = "br.com.aerozap.platformcore", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule semDependenciaDeOutrosServicos = noClasses()
            .that().resideInAPackage("br.com.aerozap.platformcore..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.aerozap.apigateway..", "br.com.aerozap.search..",
                    "br.com.aerozap.conversationai..", "br.com.aerozap.messaging..");

    @ArchTest
    static final ArchRule semCiclosEntreSubmodulos = slices()
            .matching("br.com.aerozap.platformcore.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersNaoAcessamJdbcDiretamente = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().resideInAPackage("org.springframework.jdbc..")
            .allowEmptyShould(true);
}
