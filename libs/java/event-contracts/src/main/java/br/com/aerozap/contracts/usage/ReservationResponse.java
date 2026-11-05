package br.com.aerozap.contracts.usage;

import java.time.Instant;
import java.util.Objects;

/**
 * Resposta das operações de reserva de franquia.
 *
 * <p>Franquia esgotada não é esta resposta: é erro {@code 409} com o tipo
 * {@code urn:aerozap:problem:usage-exhausted}. Assim o chamador não precisa inspecionar o corpo
 * de uma resposta de sucesso para descobrir que não pode pesquisar.
 *
 * @param reservationId identificador da reserva
 * @param status estado atual
 * @param periodId período de franquia a que a reserva pertence; a virada do trimestre não a move
 * @param remaining consultas ainda disponíveis no período depois desta operação
 * @param expiresAt quando a reserva expira se nada a confirmar ou liberar; nulo nos estados finais
 */
public record ReservationResponse(
        String reservationId,
        ReservationStatus status,
        String periodId,
        int remaining,
        Instant expiresAt) {

    public ReservationResponse {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(periodId, "periodId");
        if (remaining < 0) {
            throw new IllegalArgumentException("remaining não pode ser negativo");
        }
        if (status == ReservationStatus.RESERVED && expiresAt == null) {
            throw new IllegalArgumentException("uma reserva RESERVED precisa de expiresAt");
        }
    }
}
