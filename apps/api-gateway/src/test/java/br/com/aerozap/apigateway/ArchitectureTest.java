package br.com.aerozap.apigateway;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fronteiras da borda (AZ-015): o gateway não guarda estado de negócio, não fala com banco nem
 * com fila e não conhece os outros serviços. É isso que o mantém substituível.
 */
@AnalyzeClasses(packages = "br.com.aerozap.apigateway", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule semBancoDeDados = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.jdbc..", "javax.sql..", "java.sql..",
                    "org.springframework.data.jpa..", "jakarta.persistence..")
            .because("a borda não tem banco: regra de negócio e estado vivem nos serviços");

    @ArchTest
    static final ArchRule semMensageria = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.amqp..", "com.rabbitmq..")
            .because("a borda não publica nem consome eventos");

    @ArchTest
    static final ArchRule naoConheceOutrosServicos = noClasses()
            .that().resideInAPackage("br.com.aerozap.apigateway..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.aerozap.platformcore..", "br.com.aerozap.search..",
                    "br.com.aerozap.conversationai..", "br.com.aerozap.messaging..")
            .because("o gateway encaminha por configuração, não por código do serviço de destino");
}
