package br.com.aerozap.contracts.usage;

/**
 * Estados de uma reserva de consulta (ADR-0004).
 *
 * <p>A reserva existe para impedir que duas pesquisas simultâneas gastem a mesma última consulta
 * da franquia. Ela nasce {@code RESERVED} e termina de três formas, nunca mais de uma vez.
 */
public enum ReservationStatus {
    /** Debitada do saldo disponível, aguardando o fim da pesquisa. */
    RESERVED,
    /** A pesquisa concluiu e a consulta foi efetivamente consumida. */
    CONFIRMED,
    /** A pesquisa falhou ou foi cancelada; a consulta voltou para o saldo. */
    RELEASED,
    /** O prazo da reserva passou sem desfecho; o job de expiração devolveu a consulta. */
    EXPIRED
}
