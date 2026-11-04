package br.com.aerozap.foundation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class FoundationArchitectureTest {

    @Test
    void baseTecnicaNaoConheceNenhumServico() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.com.aerozap.foundation");
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "br.com.aerozap.platformcore..", "br.com.aerozap.apigateway..", "br.com.aerozap.search..",
                "br.com.aerozap.conversationai..", "br.com.aerozap.messaging..")
                .check(classes);
    }
}
