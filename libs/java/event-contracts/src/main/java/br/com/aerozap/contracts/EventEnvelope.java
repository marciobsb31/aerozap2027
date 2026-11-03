package br.com.aerozap.contracts;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Envelope padrão de todo evento publicado via Transactional Outbox.
 *
 * @param eventId       identificador único; é a chave de idempotência dos consumidores
 * @param eventType     tipo e chave de roteamento, por exemplo {@code search.completed}
 * @param schemaVersion versão do esquema do payload
 * @param occurredAt    instante em que o fato ocorreu
 * @param correlationId correlação de ponta a ponta (pode ser nula)
 * @param aggregateId   identificador do agregado de origem (pode ser nulo)
 * @param payload       conteúdo do evento em JSON
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String correlationId,
        String aggregateId,
        String payload) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(payload, "payload");
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType não pode ser vazio");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion deve ser >= 1");
        }
    }

    /** Cria um evento novo com identificador gerado e instante atual. */
    public static EventEnvelope of(String eventType, int schemaVersion, String correlationId,
                                   String aggregateId, String payloadJson) {
        return new EventEnvelope(UUID.randomUUID(), eventType, schemaVersion, Instant.now(),
                correlationId, aggregateId, payloadJson);
    }
}
