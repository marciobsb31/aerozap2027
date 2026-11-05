package br.com.aerozap.foundation;

import br.com.aerozap.foundation.messaging.Topology;
import br.com.aerozap.foundation.outbox.IdempotentConsumer;
import br.com.aerozap.foundation.outbox.OutboxPublisher;
import br.com.aerozap.foundation.outbox.OutboxService;
import br.com.aerozap.foundation.web.ApiExceptionHandler;
import br.com.aerozap.foundation.web.CorrelationIdFilter;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Registra automaticamente os componentes da base técnica em qualquer serviço que a inclua.
 *
 * <p>A parte web (correlação e Problem Details) vale para todo serviço. A parte de dados
 * (Outbox, mensageria e DLQ) só entra quando o serviço tem banco e RabbitMQ no classpath —
 * o API Gateway, por exemplo, não tem nenhum dos dois (AZ-015).
 */
@AutoConfiguration
public class FoundationAutoConfiguration {

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    ApiExceptionHandler apiExceptionHandler() {
        return new ApiExceptionHandler();
    }

    /** Outbox, filas e DLQ: só em serviços com banco e RabbitMQ. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({JdbcClient.class, RabbitTemplate.class})
    @EnableScheduling
    static class ComponentesDeDados {

        @Bean
        TopicExchange aerozapEventsExchange() {
            return new TopicExchange(Topology.EVENTS_EXCHANGE, true, false);
        }

        @Bean
        DirectExchange aerozapDeadLetterExchange() {
            return new DirectExchange(Topology.DEAD_LETTER_EXCHANGE, true, false);
        }

        @Bean
        OutboxService outboxService(JdbcClient jdbc) {
            return new OutboxService(jdbc);
        }

        @Bean
        OutboxPublisher outboxPublisher(JdbcClient jdbc, RabbitTemplate rabbit, PlatformTransactionManager txManager,
                                        @Value("${aerozap.outbox.batch-size:50}") int batchSize) {
            return new OutboxPublisher(jdbc, rabbit, txManager, batchSize);
        }

        @Bean
        IdempotentConsumer idempotentConsumer(JdbcClient jdbc) {
            return new IdempotentConsumer(jdbc);
        }
    }
}
