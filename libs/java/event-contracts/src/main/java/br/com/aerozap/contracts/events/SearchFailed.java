package br.com.aerozap.contracts.events;

import java.time.Instant;
import java.util.Objects;

/**
 * A pesquisa não terminou. A reserva de franquia é liberada: falha total nunca consome (ADR-0004).
 *
 * @param searchId pesquisa
 * @param userId dono
 * @param reason motivo, em forma de código estável
 * @param detail detalhe para diagnóstico; não é texto de interface
 * @param failedAt quando falhou
 */
public record SearchFailed(
        String searchId,
        String userId,
        FailureReason reason,
        String detail,
        Instant failedAt) {

    public SearchFailed {
        Objects.requireNonNull(searchId, "searchId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(failedAt, "failedAt");
    }

    /** Por que uma pesquisa falhou. */
    public enum FailureReason {
        /** Todos os provedores falharam, excederam o tempo ou recusaram. */
        ALL_PROVIDERS_FAILED,
        /** A pesquisa estourou o tempo total. */
        TIMEOUT,
        /** Erro interno da plataforma. */
        INTERNAL_ERROR,
        /** O usuário cancelou. */
        CANCELLED
    }
}
