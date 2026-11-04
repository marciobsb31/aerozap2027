package br.com.aerozap.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Critério de aceite: requisição sem token válido é rejeitada. */
class SegurancaIntegrationTest extends AbstractGatewayIntegrationTest {

    @DynamicPropertySource
    static void semLimiteDeTaxa(DynamicPropertyRegistry registro) {
        registro.add("aerozap.limite-de-taxa.habilitado", () -> "false");
    }

    @Test
    @DisplayName("sem token: 401 em Problem Details, com o código de correlação")
    void semTokenRejeita() throws Exception {
        var resposta = get("/api/plataforma/v1/system/ping", null);

        assertThat(resposta.statusCode()).isEqualTo(401);
        assertThat(resposta.headers().firstValue("Content-Type").orElse(""))
                .contains("application/problem+json");
        assertThat(resposta.body())
                .contains("urn:aerozap:problem:unauthorized")
                .contains("correlationId");
    }

    @Test
    @DisplayName("token assinado com outro segredo: 401")
    void tokenComAssinaturaErradaRejeita() throws Exception {
        String intruso = token("outro-segredo-completamente-diferente-32", EMISSOR, PUBLICO, Duration.ofMinutes(5));

        assertThat(get("/api/plataforma/v1/system/ping", intruso).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("token expirado: 401")
    void tokenExpiradoRejeita() throws Exception {
        String expirado = token(SEGREDO, EMISSOR, PUBLICO, Duration.ofSeconds(-60));

        assertThat(get("/api/plataforma/v1/system/ping", expirado).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("token de outro emissor: 401")
    void tokenDeOutroEmissorRejeita() throws Exception {
        String outro = token(SEGREDO, "https://invasor.example/identidade", PUBLICO, Duration.ofMinutes(5));

        assertThat(get("/api/plataforma/v1/system/ping", outro).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("token para outro público: 401")
    void tokenDeOutroPublicoRejeita() throws Exception {
        String outro = token(SEGREDO, EMISSOR, "outro-sistema", Duration.ofMinutes(5));

        assertThat(get("/api/plataforma/v1/system/ping", outro).statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("com token válido a requisição passa da autenticação (sem rota, 404)")
    void comTokenValidoPassaDaAutenticacao() throws Exception {
        var resposta = get("/api/caminho-sem-rota", tokenValido());

        assertThat(resposta.statusCode()).isEqualTo(404);
        assertThat(resposta.body()).contains("correlationId");
    }

    @Test
    @DisplayName("health e OpenAPI respondem sem token")
    void caminhosPublicosRespondemSemToken() throws Exception {
        assertThat(get("/actuator/health", null).statusCode()).isEqualTo(200);
        assertThat(get("/v3/api-docs", null).statusCode()).isEqualTo(200);
    }
}
