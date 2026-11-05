package br.com.aerozap.identity.sessao;

import java.time.Instant;
import java.util.UUID;

/**
 * Um refresh token guardado.
 *
 * <p>A {@code familiaId} liga todos os tokens que descendem de um mesmo login. Rotacionar cria uma
 * linha nova na mesma família; é isso que permite derrubar tudo de uma vez quando um token já usado
 * reaparece.
 *
 * @param tokenHash o hash do token, nunca o token
 */
public record Sessao(
        UUID id,
        UUID familiaId,
        UUID usuarioId,
        String tokenHash,
        Instant expiraEm,
        Instant usadoEm,
        Instant revogadoEm,
        String userAgent,
        String origemIp) {

    public boolean valida(Instant agora) {
        return revogadoEm == null && usadoEm == null && expiraEm.isAfter(agora);
    }
}
