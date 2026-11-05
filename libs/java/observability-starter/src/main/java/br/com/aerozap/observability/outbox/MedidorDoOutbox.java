package br.com.aerozap.observability.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Publica a métrica {@code aerozap.outbox.pendentes}: eventos gravados no Outbox e ainda não
 * publicados no RabbitMQ.
 *
 * <p>É o sinal mais direto de que a entrega de eventos parou: em operação normal a fila esvazia
 * em segundos. O alerta {@code OutboxAcumulando} usa esta métrica.
 */
public class MedidorDoOutbox implements MeterBinder {

    private static final String CONSULTA = "select count(*) from outbox_event where published_at is null";

    private final ObjectProvider<JdbcClient> jdbc;

    public MedidorDoOutbox(ObjectProvider<JdbcClient> jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void bindTo(MeterRegistry registro) {
        Gauge.builder("aerozap.outbox.pendentes", this::contarPendentes)
                .description("Eventos no Outbox ainda não publicados")
                .baseUnit("eventos")
                .register(registro);
    }

    private Number contarPendentes() {
        JdbcClient cliente = jdbc.getIfAvailable();
        if (cliente == null) {
            return Double.NaN;
        }
        try {
            Long total = cliente.sql(CONSULTA).query(Long.class).single();
            return total != null ? total : 0L;
        } catch (RuntimeException e) {
            // Banco indisponível ou migração ainda não aplicada: a ausência do valor já é o sinal.
            return Double.NaN;
        }
    }
}
