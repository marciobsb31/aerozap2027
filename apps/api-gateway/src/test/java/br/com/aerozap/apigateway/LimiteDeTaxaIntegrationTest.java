package br.com.aerozap.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Critério de aceite: limite de taxa comprovado por teste.
 *
 * <p>Janela longa de propósito: assim a contagem não reinicia no meio do teste e o resultado não
 * depende do relógio.
 */
class LimiteDeTaxaIntegrationTest extends AbstractGatewayIntegrationTest {

    private static final int LIMITE = 3;

    @Autowired
    private StringRedisTemplate redis;

    /** Zera os contadores: cada teste parte de uma janela limpa, em qualquer ordem de execução. */
    @BeforeEach
    void limparContadores() {
        Set<String> chaves = redis.keys("aerozap:limite:*");
        if (chaves != null && !chaves.isEmpty()) {
            redis.delete(chaves);
        }
    }

    @DynamicPropertySource
    static void limiteBaixo(DynamicPropertyRegistry registro) {
        registro.add("aerozap.limite-de-taxa.habilitado", () -> "true");
        registro.add("aerozap.limite-de-taxa.requisicoes", () -> LIMITE);
        registro.add("aerozap.limite-de-taxa.janela", () -> "1h");
    }

    @Test
    @DisplayName("passado o limite, responde 429 com Retry-After e sem contar como erro do serviço")
    void passadoOLimiteResponde429() throws Exception {
        String token = tokenValido();
        List<HttpResponse<String>> respostas = new ArrayList<>();
        for (int i = 0; i < LIMITE + 5; i++) {
            respostas.add(get("/api/caminho-sem-rota", token));
        }

        // As primeiras passam pelo limite e chegam ao roteamento (sem rota configurada: 404).
        assertThat(respostas.subList(0, LIMITE))
                .allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(404));

        List<HttpResponse<String>> excedentes = respostas.subList(LIMITE, respostas.size());
        assertThat(excedentes).allSatisfy(r -> assertThat(r.statusCode()).isEqualTo(429));

        HttpResponse<String> bloqueada = excedentes.get(0);
        assertThat(bloqueada.headers().firstValue("Retry-After")).isPresent();
        assertThat(bloqueada.headers().firstValue("X-RateLimit-Limit")).contains(String.valueOf(LIMITE));
        assertThat(bloqueada.headers().firstValue("X-RateLimit-Remaining")).contains("0");
        assertThat(bloqueada.headers().firstValue("Content-Type").orElse(""))
                .contains("application/problem+json");
        assertThat(bloqueada.body())
                .contains("urn:aerozap:problem:rate-limit")
                .contains("correlationId");
    }

    @Test
    @DisplayName("o limite também vale para requisição sem token: protege contra tentativa em massa")
    void limiteValeAntesDaAutenticacao() throws Exception {
        List<Integer> codigos = new ArrayList<>();
        for (int i = 0; i < LIMITE + 5; i++) {
            codigos.add(get("/api/caminho-sem-rota", null).statusCode());
        }

        assertThat(codigos).contains(429);
    }

    @Test
    @DisplayName("health não conta para o limite: sondagem não derruba o próprio monitoramento")
    void healthNaoContaParaOLimite() throws Exception {
        for (int i = 0; i < LIMITE + 5; i++) {
            assertThat(get("/actuator/health", null).statusCode()).isEqualTo(200);
        }
    }
}
