package br.com.aerozap.contracts.events;

import br.com.aerozap.contracts.usage.ReleaseReason;
import java.time.Instant;
import java.util.Objects;

/**
 * A reserva foi desfeita: a consulta voltou para o saldo.
 *
 * <p>É o evento que o suporte lê quando o usuário pergunta por que o saldo mudou sozinho.
 *
 * @param reservationId reserva
 * @param userId dono
 * @param searchId pesquisa
 * @param reason por quê
 * @param remaining saldo restante depois da devolução
 * @param releasedAt quando
 */
public record UsageReleased(
        String reservationId,
        String userId,
        String searchId,
        ReleaseReason reason,
        int remaining,
        Instant releasedAt) {

    public UsageReleased {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(searchId, "searchId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(releasedAt, "releasedAt");
        if (remaining < 0) {
            throw new IllegalArgumentException("remaining não pode ser negativo");
        }
    }
}
