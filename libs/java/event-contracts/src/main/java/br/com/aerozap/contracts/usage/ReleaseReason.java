package br.com.aerozap.contracts.usage;

/**
 * Por que uma reserva foi liberada. Fica na trilha imutável de uso: é o que permite ao suporte
 * explicar a um usuário por que o saldo dele voltou.
 */
public enum ReleaseReason {
    /** A pesquisa falhou (erro interno ou nenhum provedor respondeu). */
    SEARCH_FAILED,
    /** O usuário cancelou antes do fim. */
    CANCELLED,
    /** Nenhuma oferta e a política diz para não consumir (ADR-0004, regra 6). */
    NO_RESULTS,
    /** O prazo passou sem desfecho; liberada pelo job de expiração. */
    EXPIRED
}
