package br.com.aerozap.platformcore.identidade;

import br.com.aerozap.platformcore.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Jornada de autenticação de ponta a ponta, contra PostgreSQL real. */
class AutenticacaoIntegrationTest extends AbstractIntegrationTest {

    private static int sequencia;

    private static synchronized String novoEmail() {
        return "pessoa-" + (++sequencia) + "-" + System.nanoTime() + "@aerozap-teste.invalid";
    }

    private Resposta registrar(String email, String senha) {
        return postar("/v1/auth/registrar", Map.of(
                "email", email, "senha", senha, "nome", "Pessoa de Teste",
                "idioma", "pt-BR", "versaoDosTermos", "2026-11-01"));
    }

    @Test
    @DisplayName("cadastro devolve 201 com token de acesso e refresh, no envelope padrão")
    void cadastro() {
        Resposta resposta = registrar(novoEmail(), "uma senha longa o bastante");

        assertThat(resposta.status()).isEqualTo(201);
        assertThat(resposta.texto("accessToken")).contains(".");
        assertThat(resposta.texto("refreshToken")).isNotBlank();
        assertThat(resposta.corpo().get("meta")).isNotNull();
    }

    @Test
    @DisplayName("e-mail repetido devolve 409 com o tipo de problema do catálogo")
    void emailRepetido() {
        String email = novoEmail();
        registrar(email, "uma senha longa o bastante");

        Resposta repetido = registrar(email, "outra senha longa aqui");

        assertThat(repetido.status()).isEqualTo(409);
        assertThat(repetido.corpo().get("type")).isEqualTo("urn:aerozap:problem:conflict");
        assertThat(repetido.corpo().get("correlationId")).isNotNull();
    }

    @Test
    @DisplayName("maiúsculas no e-mail não criam uma segunda conta")
    void emailNormalizado() {
        String email = novoEmail();
        registrar(email, "uma senha longa o bastante");

        Resposta comMaiusculas =
                registrar(email.toUpperCase(Locale.ROOT), "uma senha longa o bastante");

        assertThat(comMaiusculas.status()).isEqualTo(409);
    }

    @Test
    @DisplayName("senha curta é recusada com 422, não com 500")
    void senhaCurta() {
        Resposta resposta = registrar(novoEmail(), "curta");

        assertThat(resposta.status()).isEqualTo(422);
        assertThat(resposta.corpo().get("type")).isEqualTo("urn:aerozap:problem:validation");
    }

    @Test
    @DisplayName("conta inexistente e senha errada devolvem exatamente a mesma resposta")
    void mesmaRespostaParaOsDoisErros() {
        String email = novoEmail();
        registrar(email, "uma senha longa o bastante");

        Resposta senhaErrada = postar("/v1/auth/entrar",
                Map.of("email", email, "senha", "senha completamente errada"));
        Resposta contaInexistente = postar("/v1/auth/entrar",
                Map.of("email", novoEmail(), "senha", "senha completamente errada"));

        // Dizer "este e-mail não existe" entrega ao atacante a lista de quem tem conta.
        assertThat(senhaErrada.status()).isEqualTo(401);
        assertThat(contaInexistente.status()).isEqualTo(401);
        assertThat(senhaErrada.corpo().get("detail"))
                .isEqualTo(contaInexistente.corpo().get("detail"));
        assertThat(senhaErrada.corpo().get("title"))
                .isEqualTo(contaInexistente.corpo().get("title"));
    }

    @Test
    @DisplayName("renovar troca o refresh token; o antigo deixa de valer")
    void rotacaoDoRefresh() {
        String primeiro = registrar(novoEmail(), "uma senha longa o bastante").texto("refreshToken");

        Resposta renovada = postar("/v1/auth/renovar", Map.of("refreshToken", primeiro));
        assertThat(renovada.status()).isEqualTo(200);
        assertThat(renovada.texto("refreshToken")).isNotEqualTo(primeiro);

        Resposta comOAntigo = postar("/v1/auth/renovar", Map.of("refreshToken", primeiro));
        assertThat(comOAntigo.status()).isEqualTo(401);
    }

    @Test
    @DisplayName("reusar um refresh já rotacionado derruba a família inteira de sessões")
    void reusoDerrubaAFamilia() {
        String primeiro = registrar(novoEmail(), "uma senha longa o bastante").texto("refreshToken");
        String segundo = postar("/v1/auth/renovar", Map.of("refreshToken", primeiro))
                .texto("refreshToken");

        // Reuso do antigo: ou vazou, ou é repetição. Não dá para distinguir, então cai tudo.
        postar("/v1/auth/renovar", Map.of("refreshToken", primeiro));

        Resposta comOValido = postar("/v1/auth/renovar", Map.of("refreshToken", segundo));
        assertThat(comOValido.status())
                .as("o token que ainda era válido também cai: a sessão pode estar comprometida")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("sair encerra a sessão")
    void sair() {
        String refresh = registrar(novoEmail(), "uma senha longa o bastante").texto("refreshToken");

        assertThat(postar("/v1/auth/sair", Map.of("refreshToken", refresh)).status()).isEqualTo(204);
        assertThat(postar("/v1/auth/renovar", Map.of("refreshToken", refresh)).status())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("o perfil vem do cabeçalho posto pelo gateway, nunca do corpo da requisição")
    void perfil() {
        String email = novoEmail();
        registrar(email, "uma senha longa o bastante");
        Resposta entrada = postar("/v1/auth/entrar",
                Map.of("email", email, "senha", "uma senha longa o bastante"));

        Resposta perfil = obter("/v1/auth/eu", "X-Usuario-Id", subDoToken(entrada.texto("accessToken")));

        assertThat(perfil.status()).isEqualTo(200);
        assertThat(perfil.texto("email")).isEqualTo(email);
        assertThat(perfil.dados().get("papeis"))
                .asInstanceOf(InstanceOfAssertFactories.list(String.class))
                .contains("USUARIO");
        assertThat(perfil.dados()).doesNotContainKeys("senhaHash", "senha");
    }

    @Test
    @DisplayName("tentativas seguidas com senha errada acabam bloqueadas")
    void forcaBrutaEBloqueada() {
        String email = novoEmail();
        registrar(email, "uma senha longa o bastante");

        List<Integer> status = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(i -> postar("/v1/auth/entrar",
                        Map.of("email", email, "senha", "errada errada errada")).status())
                .toList();

        assertThat(status).as("o limite protege a conta, não só o endereço de origem")
                .contains(429);
    }

    /** Lê o `sub` do token sem validar assinatura: aqui interessa o conteúdo, não a confiança. */
    private static String subDoToken(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                StandardCharsets.UTF_8);
        int inicio = payload.indexOf("\"sub\":\"") + 7;
        return payload.substring(inicio, payload.indexOf('"', inicio));
    }
}
