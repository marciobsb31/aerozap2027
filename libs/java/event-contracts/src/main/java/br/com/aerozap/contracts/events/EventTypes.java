package br.com.aerozap.contracts.events;

/**
 * Tipos de evento da plataforma: o valor de {@code eventType} do envelope e a chave de
 * roteamento no RabbitMQ.
 *
 * <p>O nome é contrato de verdade: está em binding de fila, em consumidor e em log. Acrescentar
 * é barato; renomear quebra consumidores em silêncio. Por isso só se acrescenta — a evolução de
 * um evento existente é feita por {@code schemaVersion}, e um tipo novo só nasce quando o fato
 * é realmente outro.
 *
 * <p>Convenção: {@code <domínio>.<fato no passado>}. Evento descreve o que aconteceu, nunca o que
 * alguém deve fazer; comando disfarçado de evento acopla quem publica a quem consome.
 */
public final class EventTypes {

    /** Versão do esquema dos payloads desta biblioteca. */
    public static final int SCHEMA_VERSION = 1;

    /** Pesquisa criada; os workers consultam os provedores. */
    public static final String SEARCH_REQUESTED = "search.requested";
    /** Pesquisa terminou; a reserva de franquia é confirmada ou liberada conforme a política. */
    public static final String SEARCH_COMPLETED = "search.completed";
    /** Pesquisa não terminou; a reserva é liberada. */
    public static final String SEARCH_FAILED = "search.failed";

    /** Consulta reservada na franquia do usuário. */
    public static final String USAGE_RESERVED = "usage.reserved";
    /** Reserva confirmada: a consulta foi consumida. */
    public static final String USAGE_CONFIRMED = "usage.confirmed";
    /** Reserva liberada: a consulta voltou para o saldo. */
    public static final String USAGE_RELEASED = "usage.released";

    /** Um alerta de preço bateu o alvo do usuário. */
    public static final String PRICE_TARGET_REACHED = "alert.price-target-reached";

    /** Mensagem recebida em um canal de entrada (WhatsApp, por exemplo). */
    public static final String MESSAGE_RECEIVED = "message.received";

    private EventTypes() {
    }
}
