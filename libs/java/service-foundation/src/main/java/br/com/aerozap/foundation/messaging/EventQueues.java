package br.com.aerozap.foundation.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;

/** Fábrica de filas de consumidores: fila durável com DLQ associada. */
public final class EventQueues {

    private EventQueues() {
    }

    /**
     * Cria a fila {@code queueName} ligada ao exchange de eventos por {@code bindingKey} e a fila
     * {@code queueName.dlq}, que recebe as mensagens rejeitadas depois das tentativas.
     * Declare o resultado como um bean {@code Declarables}.
     */
    public static Declarables durableWithDlq(String queueName, String bindingKey) {
        String dlqName = queueName + ".dlq";
        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(Topology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(dlqName)
                .build();
        Queue dlq = QueueBuilder.durable(dlqName).build();
        return new Declarables(
                queue,
                dlq,
                new Binding(queueName, Binding.DestinationType.QUEUE, Topology.EVENTS_EXCHANGE, bindingKey, null),
                new Binding(dlqName, Binding.DestinationType.QUEUE, Topology.DEAD_LETTER_EXCHANGE, dlqName, null));
    }
}
