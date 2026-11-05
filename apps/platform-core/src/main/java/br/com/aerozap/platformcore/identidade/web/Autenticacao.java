package br.com.aerozap.platformcore.identidade.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * O que entra e o que sai da autenticação.
 *
 * <p>Nenhum destes registros carrega hash de senha, e nenhum devolve o e-mail de outra pessoa.
 * O {@code refreshToken} sai uma vez e só; o servidor guarda o hash dele.
 */
public final class Autenticacao {

    private Autenticacao() {
    }

    public record PedidoDeCadastro(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 10, max = 200) String senha,
            @Size(max = 200) String nome,
            @Size(max = 10) String idioma,
            @NotBlank @Size(max = 40) String versaoDosTermos) {
    }

    public record PedidoDeEntrada(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 200) String senha) {
    }

    public record PedidoDeRenovacao(@NotBlank String refreshToken) {
    }

    public record Sessao(
            String accessToken,
            String refreshToken,
            long expiraEmSegundos,
            Instant sessaoExpiraEm) {
    }

    public record Perfil(String id, String email, String nome, String idioma, List<String> papeis) {
    }
}
