package br.com.aerozap.conversationai;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fronteiras do serviço de conversa (AZ-019).
 *
 * <p>A regra da allowlist de ferramentas (RF-046) vira teste aqui: o pacote de interpretação não
 * pode alcançar rede, banco, fila nem o sistema de arquivos. Se amanhã alguém der uma "ferramenta"
 * ao interpretador, o build quebra antes de a ferramenta existir em produção.
 */
@AnalyzeClasses(packages = "br.com.aerozap.conversationai", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule interpretacaoNaoAlcancaFerramentas = noClasses()
            .that().resideInAPackage("br.com.aerozap.conversationai.interpretacao..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "java.net..", "java.nio.file..", "java.sql..",
                    "org.springframework.web.client..", "org.springframework.jdbc..",
                    "org.springframework.amqp..", "com.rabbitmq..")
            .because("o interpretador não chama ferramenta nenhuma: ele lê texto e devolve "
                    + "pesquisa, pergunta ou recusa");

    @ArchTest
    static final ArchRule interpretacaoNaoDependeDeFramework = noClasses()
            .that().resideInAPackage("br.com.aerozap.conversationai.interpretacao..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.context..", "org.springframework.boot..",
                    "org.springframework.stereotype..")
            .because("o interpretador é Java puro: dá para testá-lo e trocá-lo sem subir o serviço");

    @ArchTest
    static final ArchRule naoConheceOutrosServicos = noClasses()
            .that().resideInAPackage("br.com.aerozap.conversationai..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.aerozap.platformcore..", "br.com.aerozap.apigateway..",
                    "br.com.aerozap.search..", "br.com.aerozap.messaging..");
}
