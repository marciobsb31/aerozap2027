package br.com.aerozap.foundation.outbox;

import br.com.aerozap.foundation.messaging.Topology;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publica os eventos pendentes do outbox no RabbitMQ, em lotes, com entrega "pelo menos uma vez".
 * Várias instâncias podem rodar juntas: {@code for update skip locked} evita que disputem as mesmas linhas.
 * Se o processo cair entre enviar e marcar, o evento é reenviado: os consumidores são idempotentes.
 */
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private record Row(UUID id, String eventType, int schemaVersion, String correlationId,
                       String aggregateId, String payload) {
    }

    private final JdbcClient jdbc;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate tx;
    private final int batchSize;

    public OutboxPublisher(JdbcClient jdbc, RabbitTemplate rabbit, PlatformTransactionManager txManager,
                           int batchSize) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.tx = new TransactionTemplate(txManager);
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${aerozap.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        int published;
        do {
            Integer result = tx.execute(status -> publishBatch());
            published = result == null ? 0 : result;
        } while (published >= batchSize);
    }

    int publishBatch() {
        List<Row> rows = jdbc.sql("""
                select id, event_type, schema_version, correlation_id, aggregate_id, payload
                from outbox_event
                where published_at is null
                order by occurred_at, id
                limit :n
                for update skip locked
                """)
                .param("n", batchSize)
                .query((rs, i) -> new Row(rs.getObject("id", UUID.class), rs.getString("event_type"),
                        rs.getInt("schema_version"), rs.getString("correlation_id"),
                        rs.getString("aggregate_id"), rs.getString("payload")))
                .list();

        for (Row row : rows) {
            var builder = MessageBuilder.withBody(row.payload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setMessageId(row.id().toString())
                    .setHeader("x-schema-version", row.schemaVersion());
            if (row.correlationId() != null) {
                builder.setCorrelationId(row.correlationId());
            }
            if (row.aggregateId() != null) {
                builder.setHeader("x-aggregate-id", row.aggregateId());
            }
            Message message = builder.build();
            rabbit.send(Topology.EVENTS_EXCHANGE, row.eventType(), message);
            jdbc.sql("update outbox_event set published_at = now() where id = :id")
                    .param("id", row.id())
                    .update();
        }
        if (!rows.isEmpty()) {
            log.debug("Outbox: {} evento(s) publicado(s)", rows.size());
        }
        return rows.size();
    }
}
