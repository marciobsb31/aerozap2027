package br.com.aerozap.contracts.events;

import java.time.Instant;
import java.util.Objects;

/**
 * Uma consulta foi reservada na franquia do usuário.
 *
 * <p>Publicado para a trilha de uso e para a interface atualizar o saldo. A pesquisa não depende
 * dele: a reserva em si é síncrona (REST), porque a pesquisa não pode começar sem a certeza de
 * que há saldo.
 *
 * @param reservationId reserva
 * @param userId dono da franquia
 * @param searchId pesquisa que motivou a reserva
 * @param periodId período de franquia
 * @param remaining saldo restante depois da reserva
 * @param reservedAt quando
 * @param expiresAt prazo para confirmar ou liberar
 */
public record UsageReserved(
        String reservationId,
        String userId,
        String searchId,
        String periodId,
        int remaining,
        Instant reservedAt,
        Instant expiresAt) {

    public UsageReserved {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(searchId, "searchId");
        Objects.requireNonNull(periodId, "periodId");
        Objects.requireNonNull(reservedAt, "reservedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (remaining < 0) {
            throw new IllegalArgumentException("remaining não pode ser negativo");
        }
        if (expiresAt.isBefore(reservedAt)) {
            throw new IllegalArgumentException("expiresAt não pode ser anterior a reservedAt");
        }
    }
}
