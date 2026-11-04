package br.com.aerozap.observability;

import br.com.aerozap.observability.outbox.MedidorDoOutbox;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Registra as métricas próprias do AeroZap em qualquer serviço que inclua esta biblioteca.
 *
 * <p>Métricas do Actuator, exportação Prometheus e rastros OTLP vêm das configurações
 * automáticas do próprio Spring Boot; aqui entram apenas os medidores do domínio técnico.
 */
@AutoConfiguration
@ConditionalOnClass(JdbcClient.class)
public class ObservabilityAutoConfiguration {

    /** O medidor tolera a ausência do JdbcClient: serviços sem banco simplesmente não publicam o valor. */
    @Bean
    MeterBinder medidorDoOutbox(ObjectProvider<JdbcClient> jdbc) {
        return new MedidorDoOutbox(jdbc);
    }
}
