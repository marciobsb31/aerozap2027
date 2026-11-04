package br.com.aerozap.foundation.web;

import java.time.Instant;

/** Envelope padrão de resposta de sucesso. Erros usam Problem Details (RFC 9457). */
public record ApiResponse<T>(T data, Meta meta) {

    public record Meta(String correlationId, Instant timestamp) {
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, new Meta(CorrelationIds.current(), Instant.now()));
    }
}
