package br.com.aerozap.foundation.messaging;

/** Nomes da topologia de mensageria compartilhada pelos serviços. */
public final class Topology {

    /** Exchange de eventos de domínio (tópico). A chave de roteamento é o eventType. */
    public static final String EVENTS_EXCHANGE = "aerozap.events";

    /** Exchange das filas de mensagens mortas (DLQ). */
    public static final String DEAD_LETTER_EXCHANGE = "aerozap.events.dlx";

    private Topology() {
    }
}
