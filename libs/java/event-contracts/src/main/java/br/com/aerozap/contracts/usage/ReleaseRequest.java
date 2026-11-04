package br.com.aerozap.contracts.usage;

import java.util.Objects;

/**
 * Pedido de liberação de uma reserva.
 * {@code POST /internal/usage/reservations/{searchId}/release} (ADR-0004).
 *
 * @param reason por que a consulta volta para o saldo; entra na trilha de auditoria
 */
public record ReleaseRequest(ReleaseReason reason) {

    public ReleaseRequest {
        Objects.requireNonNull(reason, "reason");
    }
}
