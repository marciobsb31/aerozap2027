package br.com.aerozap.identity.usuario;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Uma conta.
 *
 * @param senhaHash nunca a senha; o hash. Este campo não sai do serviço e não entra em resposta.
 */
public record Usuario(
        UUID id,
        String email,
        String senhaHash,
        String nome,
        String idioma,
        Estado estado,
        List<String> papeis,
        Instant criadoEm) {

    public enum Estado {
        ATIVO,
        BLOQUEADO
    }

    public boolean podeEntrar() {
        return estado == Estado.ATIVO;
    }

    /** Normalização de e-mail: a mesma usada no índice único do banco. */
    public static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
