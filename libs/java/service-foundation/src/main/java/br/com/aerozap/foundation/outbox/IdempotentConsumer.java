package br.com.aerozap.foundation.outbox;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garante efeito único por (evento, consumidor), mesmo que a mensagem chegue mais de uma vez.
 * O registro de "já processado" e o efeito acontecem na mesma transação: se o efeito falhar,
 * o registro é desfeito e a nova tentativa processa de novo.
 */
public class IdempotentConsumer {

    private final JdbcClient jdbc;

    public IdempotentConsumer(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @return {@code true} se executou o efeito; {@code false} se o evento já havia sido processado
     */
    @Transactional
    public boolean process(UUID eventId, String consumerName, Runnable effect) {
        int inserted = jdbc.sql("""
                insert into processed_event (event_id, consumer)
                values (:eventId, :consumer)
                on conflict do nothing
                """)
                .param("eventId", eventId)
                .param("consumer", consumerName)
                .update();
        if (inserted == 0) {
            return false;
        }
        effect.run();
        return true;
    }
}
