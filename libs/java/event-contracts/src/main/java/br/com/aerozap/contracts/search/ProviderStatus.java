package br.com.aerozap.contracts.search;

/**
 * Como terminou a consulta a um provedor.
 *
 * <p>A distinção existe para a degradação controlada (RF-027) e para a política de consumo de
 * franquia (ADR-0004): "ninguém respondeu" e "responderam, não há voo" levam a decisões
 * diferentes sobre debitar ou não a consulta do usuário.
 */
public enum ProviderStatus {
    /** Respondeu com ofertas. */
    OK,
    /** Respondeu, sem nenhuma oferta para os critérios. */
    EMPTY,
    /** Respondeu parte do pedido (por exemplo, só um dos sentidos). */
    PARTIAL,
    /** Erro do provedor ou da integração. */
    ERROR,
    /** Não respondeu dentro do tempo limite. */
    TIMEOUT,
    /** Recusou por excesso de chamadas. */
    RATE_LIMITED,
    /** Credencial inválida, expirada ou sem direito a este recurso. */
    UNAUTHORIZED
}
