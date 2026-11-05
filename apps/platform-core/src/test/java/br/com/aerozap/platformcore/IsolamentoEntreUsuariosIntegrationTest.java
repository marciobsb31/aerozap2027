package br.com.aerozap.platformcore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Dois usuários de verdade, e cada um só enxerga o que é dele (RF-007).
 *
 * <p>Aqui o cabeçalho {@code X-Usuario-Id} é o que a borda preencheu a partir do token. Que o
 * cliente não consegue forjá-lo é provado em {@code EncaminhamentoIntegrationTest}, no gateway, e de
 * ponta a ponta em {@code scripts/dev-isolamento-check.sh}.
 */
class IsolamentoEntreUsuariosIntegrationTest extends AbstractIntegrationTest {

    private static final String SENHA = "uma senha longa o bastante";

    private record Pessoa(String id, String email, String acesso, String refresh) {
    }

    private Pessoa ana;
    private Pessoa bia;

    private Pessoa registrar(String nome) {
        String email = nome + "-" + UUID.randomUUID() + "@exemplo.test";
        Resposta resposta = postar("/api/v1/auth/registrar", Map.of(
                "email", email, "senha", SENHA, "nome", nome, "idioma", "pt-BR",
                "versaoDosTermos", "2026-11-01"));
        assertThat(resposta.status()).as(resposta.corpo().toString()).isIn(200, 201);
        String acesso = resposta.texto("accessToken");
        return new Pessoa(sub(acesso), email, acesso, resposta.texto("refreshToken"));
    }

    private static String sub(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        return payload.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
    }

    @BeforeEach
    void criarAsDuasPessoas() {
        ana = registrar("ana");
        bia = registrar("bia");
    }

    @Test
    @DisplayName("o perfil devolvido é sempre o de quem está no cabeçalho da borda")
    void perfilDeCadaUm() {
        Resposta daAna = obter("/api/v1/auth/eu", "X-Usuario-Id", ana.id());
        Resposta daBia = obter("/api/v1/auth/eu", "X-Usuario-Id", bia.id());

        assertThat(daAna.corpo().toString()).contains(ana.email()).doesNotContain(bia.email());
        assertThat(daBia.corpo().toString()).contains(bia.email()).doesNotContain(ana.email());
    }

    @Test
    @DisplayName("sem identidade da borda não há perfil nem saldo, e ninguém recebe 500")
    void semIdentidade() {
        assertThat(obter("/api/v1/auth/eu").status()).isBetween(400, 499);
        assertThat(obter("/api/v1/usage/saldo").status()).isBetween(400, 499);
        assertThat(obter("/api/v1/auth/eu", "X-Usuario-Id", "nao-e-um-id").status()).isBetween(400, 499);
    }

    @Test
    @DisplayName("consumir a franquia de uma pessoa não mexe no saldo da outra")
    void saldoDeCadaUm() {
        assertThat(postar("/internal/usage/reservations", Map.of(
                "userId", ana.id(), "searchId", "busca-1", "idempotencyKey", "chave-1")).status()).isEqualTo(200);

        Resposta saldoDaAna = obter("/api/v1/usage/saldo", "X-Usuario-Id", ana.id());
        Resposta saldoDaBia = obter("/api/v1/usage/saldo", "X-Usuario-Id", bia.id());

        assertThat(saldoDaAna.dados().get("restantes")).isEqualTo(4);
        assertThat(saldoDaBia.dados().get("restantes")).isEqualTo(5);
        assertThat(saldoDaAna.dados().get("periodoId")).isNotEqualTo(saldoDaBia.dados().get("periodoId"));
    }

    @Test
    @DisplayName("a mesma chave de idempotência em pessoas diferentes são duas reservas")
    void chaveNaoCruzaUsuarios() {
        Resposta daAna = postar("/internal/usage/reservations", Map.of(
                "userId", ana.id(), "searchId", "mesma-busca", "idempotencyKey", "mesma-chave"));
        Resposta daBia = postar("/internal/usage/reservations", Map.of(
                "userId", bia.id(), "searchId", "mesma-busca", "idempotencyKey", "mesma-chave"));

        assertThat(daAna.status()).isEqualTo(200);
        assertThat(daBia.status()).as("a Bia não pode receber a reserva da Ana").isEqualTo(200);
        assertThat(daAna.texto("reservationId")).isNotEqualTo(daBia.texto("reservationId"));
    }

    @Test
    @DisplayName("reuso de refresh derruba a família de quem reusou, e a sessão da outra pessoa segue viva")
    void reusoNaoAtingeOutraPessoa() {
        String segundo = postar("/api/v1/auth/renovar", Map.of("refreshToken", ana.refresh())).texto("refreshToken");
        assertThat(postar("/api/v1/auth/renovar", Map.of("refreshToken", ana.refresh())).status()).isEqualTo(401);

        assertThat(postar("/api/v1/auth/renovar", Map.of("refreshToken", segundo)).status())
                .as("a família da Ana caiu").isEqualTo(401);
        assertThat(postar("/api/v1/auth/renovar", Map.of("refreshToken", bia.refresh())).status())
                .as("a Bia não tem nada com isso").isEqualTo(200);
    }

    @Test
    @DisplayName("sair com o token de uma pessoa não encerra a sessão da outra")
    void sairNaoAtingeOutraPessoa() {
        postar("/api/v1/auth/sair", Map.of("refreshToken", ana.refresh()));

        assertThat(postar("/api/v1/auth/renovar", Map.of("refreshToken", ana.refresh())).status()).isEqualTo(401);
        assertThat(postar("/api/v1/auth/renovar", Map.of("refreshToken", bia.refresh())).status()).isEqualTo(200);
    }
}
