package br.com.aerozap.platformcore.identidade;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Fronteiras do serviço de identidade. */
@AnalyzeClasses(packages = "br.com.aerozap.platformcore",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaDaIdentidadeTest {

    /**
     * A camada web não fala com o banco. Entre o controlador e o JdbcClient tem de haver um
     * serviço — é onde mora a transação, e SQL dentro de controlador é o atalho que faz a
     * transação sumir sem ninguém notar.
     */
    @ArchTest
    static final ArchRule webNaoAcessaBanco = noClasses()
            .that().resideInAPackage("..identidade.web..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.jdbc..");

    /** Nada do domínio depende da camada web: o serviço não sabe que existe HTTP. */
    @ArchTest
    static final ArchRule dominioNaoDependeDaWeb = noClasses()
            .that().resideInAnyPackage("..identidade.usuario..", "..identidade.sessao..")
            .should().dependOnClassesThat().resideInAPackage("..identidade.web..");

    /**
     * Ninguém registra a senha nem o token em log. A regra pega o caso óbvio — chamar o logger
     * direto da classe que manipula segredo —, e não substitui revisão; é a trava barata.
     */
    @ArchTest
    static final ArchRule segredosNaoVaoParaOLog = noClasses()
            .that().haveSimpleName("Senhas").or().haveSimpleName("Segredos")
            .should().dependOnClassesThat().resideInAPackage("org.slf4j..");
}
