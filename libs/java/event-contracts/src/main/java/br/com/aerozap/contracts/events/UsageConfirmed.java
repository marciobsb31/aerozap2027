package br.com.aerozap.contracts.events;

import java.time.Instant;
import java.util.Objects;

/**
 * A reserva virou consumo: a consulta saiu do saldo para sempre.
 *
 * @param reservationId reserva
 * @param userId dono
 * @param searchId pesquisa
 * @param remaining saldo restante
 * @param confirmedAt quando
 */
public record UsageConfirmed(
        String reservationId,
        String userId,
        String searchId,
        int remaining,
        Instant confirmedAt) {

    public UsageConfirmed {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(searchId, "searchId");
        Objects.requireNonNull(confirmedAt, "confirmedAt");
        if (remaining < 0) {
            throw new IllegalArgumentException("remaining não pode ser negativo");
        }
    }
}
