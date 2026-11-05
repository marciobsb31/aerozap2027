package br.com.aerozap.contracts.usage;

import java.util.Objects;

/**
 * Pedido de reserva de uma consulta da franquia.
 * {@code POST /internal/usage/reservations} (ADR-0004).
 *
 * <p>Dois campos de unicidade, de propósito: {@code searchId} garante uma reserva por pesquisa,
 * e {@code idempotencyKey} protege a repetição da própria chamada — reentrega de mensagem,
 * retentativa de rede, worker reiniciado. Repetir com a mesma chave devolve a mesma reserva,
 * sem novo débito.
 *
 * @param userId dono da franquia; vem sempre do token, nunca do corpo da requisição do usuário
 * @param searchId pesquisa que motivou a reserva
 * @param idempotencyKey chave da tentativa
 */
public record ReservationRequest(String userId, String searchId, String idempotencyKey) {

    public ReservationRequest {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(searchId, "searchId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        if (userId.isBlank() || searchId.isBlank() || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("userId, searchId e idempotencyKey não podem ser vazios");
        }
    }
}
