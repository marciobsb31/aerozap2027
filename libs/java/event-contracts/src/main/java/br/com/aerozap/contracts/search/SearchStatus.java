package br.com.aerozap.contracts.search;

/**
 * Estados de uma pesquisa (RF-025). A pesquisa é assíncrona: o cliente recebe o identificador
 * imediatamente e acompanha a mudança de estado.
 *
 * <p>{@code COMPLETED} significa "terminou", não "achou alguma coisa": uma pesquisa pode concluir
 * sem ofertas. O consumo de franquia nesse caso segue a política configurável da ADR-0004.
 */
public enum SearchStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
