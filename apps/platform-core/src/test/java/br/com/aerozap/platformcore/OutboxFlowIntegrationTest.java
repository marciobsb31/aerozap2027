package br.com.aerozap.platformcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import br.com.aerozap.contracts.EventEnvelope;
import br.com.aerozap.foundation.messaging.EventQueues;
import br.com.aerozap.foundation.messaging.Topology;
import br.com.aerozap.foundation.outbox.IdempotentConsumer;
import br.com.aerozap.foundation.outbox.OutboxService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Critério de aceite da AZ-014: evento publicado via Outbox e consumido sem duplicar efeito; DLQ funciona. */
class OutboxFlowIntegrationTest extends AbstractIntegrationTest {

    public static class DemoListeners {
        final AtomicInteger received = new AtomicInteger();
        final AtomicInteger effects = new AtomicInteger();
        final AtomicInteger failedAttempts = new AtomicInteger();
        private final IdempotentConsumer consumer;

        DemoListeners(IdempotentConsumer consumer) {
            this.consumer = consumer;
        }

        @RabbitListener(queues = "test.demo")
        public void onDemo(Message message) {
            received.incrementAndGet();
            UUID eventId = UUID.fromString(message.getMessageProperties().getMessageId());
            consumer.process(eventId, "test.demo", effects::incrementAndGet);
        }

        @RabbitListener(queues = "test.fail")
        public void onFail(Message message) {
            failedAttempts.incrementAndGet();
            throw new IllegalStateException("falha proposital do teste");
        }
    }

    @TestConfiguration
    static class TestMessaging {
        @Bean
        Declarables demoQueue() {
            return EventQueues.durableWithDlq("test.demo", "demo.*");
        }

        @Bean
        Declarables failingQueue() {
            return EventQueues.durableWithDlq("test.fail", "fail.*");
        }

        @Bean
        DemoListeners demoListeners(IdempotentConsumer consumer) {
            return new DemoListeners(consumer);
        }
    }

    @Autowired
    OutboxService outbox;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    RabbitTemplate rabbit;
    @Autowired
    PlatformTransactionManager txManager;
    @Autowired
    DemoListeners listeners;

    private int count(String sql, UUID id) {
        return jdbc.sql(sql).param("id", id).query(Integer.class).single();
    }

    @Test
    void eventoViaOutboxEhConsumidoUmaVezMesmoComMensagemDuplicada() {
        var tx = new TransactionTemplate(txManager);
        var event = EventEnvelope.of("demo.created", 1, "corr-1", "agg-1", "{\"ok\":true}");

        tx.executeWithoutResult(status -> outbox.append(event));

        await().atMost(Duration.ofSeconds(30)).until(() -> listeners.effects.get() == 1);
        await().atMost(Duration.ofSeconds(10)).until(() -> count(
                "select count(*) from outbox_event where id = :id and published_at is not null", event.eventId()) == 1);

        // Entrega duplicada da MESMA mensagem (como em reenvio após queda): o efeito não pode repetir.
        Message duplicate = MessageBuilder.withBody("{\"ok\":true}".getBytes(StandardCharsets.UTF_8))
                .setMessageId(event.eventId().toString())
                .build();
        rabbit.send(Topology.EVENTS_EXCHANGE, "demo.created", duplicate);

        await().atMost(Duration.ofSeconds(30)).until(() -> listeners.received.get() >= 2);
        assertThat(listeners.effects.get()).isEqualTo(1);
        assertThat(count("select count(*) from processed_event where event_id = :id", event.eventId())).isEqualTo(1);
    }

    @Test
    void eventoNaoExisteSeATransacaoDeNegocioFizerRollback() {
        var tx = new TransactionTemplate(txManager);
        var event = EventEnvelope.of("demo.created", 1, "corr-2", "agg-2", "{}");

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            outbox.append(event);
            throw new IllegalStateException("rollback proposital");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(count("select count(*) from outbox_event where id = :id", event.eventId())).isZero();
    }

    @Test
    void appendSemTransacaoEhRecusado() {
        var event = EventEnvelope.of("demo.created", 1, null, null, "{}");
        assertThatThrownBy(() -> outbox.append(event)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void mensagemQueFalhaVaiParaADlqDepoisDasTentativas() {
        var tx = new TransactionTemplate(txManager);
        var event = EventEnvelope.of("fail.created", 1, "corr-4", "agg-4", "{}");

        tx.executeWithoutResult(status -> outbox.append(event));

        Message dead = null;
        long deadline = System.currentTimeMillis() + 45_000;
        while (dead == null && System.currentTimeMillis() < deadline) {
            dead = rabbit.receive("test.fail.dlq", 2_000);
        }
        assertThat(dead).as("mensagem na DLQ").isNotNull();
        assertThat(dead.getMessageProperties().getMessageId()).isEqualTo(event.eventId().toString());
        assertThat(listeners.failedAttempts.get()).isGreaterThanOrEqualTo(3);
    }
}
