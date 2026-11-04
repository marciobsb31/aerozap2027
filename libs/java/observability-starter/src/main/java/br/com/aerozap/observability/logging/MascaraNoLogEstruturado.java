package br.com.aerozap.observability.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Aplica o mascaramento a todos os valores de texto do log estruturado (JSON) do Spring Boot:
 * mensagem, valores do MDC e pares chave-valor.
 *
 * <p>Registrado em {@code META-INF/spring.factories}, portanto vale para qualquer serviço que
 * inclua esta biblioteca e ligue {@code logging.structured.format.console} ou
 * {@code logging.structured.format.file}.
 */
public class MascaraNoLogEstruturado implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {

    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> membros) {
        membros.applyingValueProcessor(
                JsonWriter.ValueProcessor.of(String.class, MascaradorDeDadosSensiveis::mascarar));
    }
}
