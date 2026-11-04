package br.com.aerozap.contracts.search;

import java.time.Duration;
import java.util.Objects;

/**
 * O que o adaptador de provedor precisa saber além da intenção: identidade da pesquisa, prazo e
 * rastreabilidade.
 *
 * <p>Não traz nada sobre quem pesquisou. O adaptador fala com um serviço externo; dado de pessoa
 * não atravessa essa fronteira sem necessidade (AGENTS, privacidade).
 *
 * @param searchId identificador da pesquisa, usado nos logs e na correlação com as ofertas
 * @param correlationId correlação de ponta a ponta
 * @param timeout prazo máximo para este provedor responder
 */
public record SearchContext(String searchId, String correlationId, Duration timeout) {

    public SearchContext {
        Objects.requireNonNull(searchId, "searchId");
        Objects.requireNonNull(timeout, "timeout");
        if (searchId.isBlank()) {
            throw new IllegalArgumentException("searchId não pode ser vazio");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout precisa ser positivo");
        }
    }
}
