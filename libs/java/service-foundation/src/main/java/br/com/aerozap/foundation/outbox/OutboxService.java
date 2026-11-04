package br.com.aerozap.foundation.outbox;

import br.com.aerozap.contracts.EventEnvelope;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional Outbox: grava o evento na MESMA transação da mudança de negócio.
 * A publicação no broker é feita depois, pelo {@link OutboxPublisher}.
 */
public class OutboxService {

    private final JdbcClient jdbc;

    public OutboxService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Exige transação ativa: o evento só existe se a mudança de negócio for confirmada. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(EventEnvelope event) {
        jdbc.sql("""
                insert into outbox_event
                  (id, event_type, schema_version, occurred_at, correlation_id, aggregate_id, payload)
                values (:id, :type, :version, :occurredAt, :correlationId, :aggregateId, :payload)
                """)
                .param("id", event.eventId())
                .param("type", event.eventType())
                .param("version", event.schemaVersion())
                .param("occurredAt", OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                .param("correlationId", event.correlationId())
                .param("aggregateId", event.aggregateId())
                .param("payload", event.payload())
                .update();
    }
}
