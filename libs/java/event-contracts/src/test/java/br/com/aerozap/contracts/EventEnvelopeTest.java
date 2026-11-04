package br.com.aerozap.contracts;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    @Test
    void criaEventoComIdentificadorEInstante() {
        var e = EventEnvelope.of("demo.created", 1, "corr-1", "agg-1", "{}");
        assertNotNull(e.eventId());
        assertNotNull(e.occurredAt());
        assertEquals("demo.created", e.eventType());
    }

    @Test
    void rejeitaTipoVazioEVersaoInvalida() {
        assertThrows(IllegalArgumentException.class, () -> EventEnvelope.of(" ", 1, null, null, "{}"));
        assertThrows(IllegalArgumentException.class, () -> EventEnvelope.of("x", 0, null, null, "{}"));
    }

    @Test
    void contratosNaoDependemDeFrameworks() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.com.aerozap.contracts");
        noClasses().should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..")
                .check(classes);
    }
}
