package br.com.aerozap.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Encaminhamento: o gateway repassa método, caminho, parâmetros, corpo e status, e acrescenta a
 * correlação. O destino é um servidor de teste que ecoa o que recebeu — assim o teste mede o
 * gateway, não o serviço.
 */
class EncaminhamentoIntegrationTest extends AbstractGatewayIntegrationTest {

    private static final HttpServer SERVICO_DE_TESTE;

    static {
        try {
            SERVICO_DE_TESTE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SERVICO_DE_TESTE.createContext("/", EncaminhamentoIntegrationTest::responder);
            SERVICO_DE_TESTE.setExecutor(Executors.newFixedThreadPool(4));
            SERVICO_DE_TESTE.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void responder(HttpExchange troca) throws IOException {
        String caminho = troca.getRequestURI().getPath();
        if (caminho.endsWith("/lento")) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        byte[] corpoRecebido = troca.getRequestBody().readAllBytes();
        String resumo = "metodo=" + troca.getRequestMethod()
                + " caminho=" + caminho
                + " consulta=" + String.valueOf(troca.getRequestURI().getQuery())
                + " correlacao=" + String.valueOf(troca.getRequestHeaders().getFirst("X-Correlation-Id"))
                + " encaminhado-por=" + String.valueOf(troca.getRequestHeaders().getFirst("X-Forwarded-For"))
                + " autorizacao=" + (troca.getRequestHeaders().getFirst("Authorization") != null)
                + " usuario=" + troca.getRequestHeaders().get("X-Usuario-Id")
                + " corpo=" + new String(corpoRecebido, StandardCharsets.UTF_8);

        int status = caminho.endsWith("/erro") ? 418 : 200;
        byte[] saida = resumo.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        troca.getResponseHeaders().add("X-Servico-De-Teste", "sim");
        troca.sendResponseHeaders(status, saida.length);
        troca.getResponseBody().write(saida);
        troca.close();
    }

    @DynamicPropertySource
    static void rotaParaOServicoDeTeste(DynamicPropertyRegistry registro) {
        registro.add("aerozap.limite-de-taxa.habilitado", () -> "false");
        registro.add("aerozap.gateway.rotas[0].prefixo", () -> "/api/teste");
        registro.add("aerozap.gateway.rotas[0].destino",
                () -> "http://127.0.0.1:" + SERVICO_DE_TESTE.getAddress().getPort() + "/api/teste");
        registro.add("aerozap.gateway.rotas[1].prefixo", () -> "/api/plataforma");
        registro.add("aerozap.gateway.rotas[1].destino",
                () -> "http://127.0.0.1:" + SERVICO_DE_TESTE.getAddress().getPort() + "/api");
        registro.add("aerozap.gateway.tempos-limite.leitura", () -> "600ms");
    }

    @Test
    @DisplayName("repassa método, caminho, parâmetros e a correlação recebida")
    void repassaRequisicaoComCorrelacao() throws Exception {
        var resposta = get("/api/teste/eco?pagina=2", tokenValido(), "X-Correlation-Id", "correlacao-de-teste");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body())
                .contains("metodo=GET")
                .contains("caminho=/api/teste/eco")
                .contains("consulta=pagina=2")
                .contains("correlacao=correlacao-de-teste")
                .contains("encaminhado-por=127.0.0.1")
                .contains("autorizacao=true");
        assertThat(resposta.headers().firstValue("X-Servico-De-Teste")).contains("sim");
        assertThat(resposta.headers().firstValue("X-Correlation-Id")).contains("correlacao-de-teste");
    }

    @Test
    @DisplayName("repassa o corpo do POST sem interpretar")
    void repassaOCorpo() throws Exception {
        var resposta = chamar("POST", "/api/teste/eco", tokenValido(), "{\"origem\":\"whatsapp\"}",
                "Content-Type", "application/json");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).contains("metodo=POST").contains("corpo={\"origem\":\"whatsapp\"}");
    }

    @Test
    @DisplayName("devolve o status do serviço como ele veio, sem traduzir")
    void repassaOStatusDoServico() throws Exception {
        assertThat(get("/api/teste/erro", tokenValido()).statusCode()).isEqualTo(418);
    }

    @Test
    @DisplayName("serviço lento vira 504, não espera para sempre")
    void servicoLentoViraTempoEsgotado() throws Exception {
        var resposta = get("/api/teste/lento", tokenValido());

        assertThat(resposta.statusCode()).isEqualTo(504);
        assertThat(resposta.body()).contains("correlationId");
    }

    @Test
    @DisplayName("caminho sem rota configurada: 404")
    void caminhoSemRotaDa404() throws Exception {
        assertThat(get("/api/outra-coisa/x", tokenValido()).statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("quem o serviço enxerga vem do token, nunca de um cabeçalho do cliente (RF-007)")
    void usuarioVemDoToken() throws Exception {
        var semForjar = get("/api/teste/eco", tokenValido());
        assertThat(semForjar.body()).contains("usuario=[usuario-de-teste]");

        var forjando = get("/api/teste/eco", tokenValido(), "X-Usuario-Id", "id-da-vitima");
        assertThat(forjando.body())
                .contains("usuario=[usuario-de-teste]")
                .doesNotContain("id-da-vitima");

        var emMinusculas = get("/api/teste/eco", tokenValido(), "x-usuario-id", "id-da-vitima");
        assertThat(emMinusculas.body()).doesNotContain("id-da-vitima");
    }

    @Test
    @DisplayName("entrar, registrar, renovar e sair passam sem token, e sem carregar identidade forjada")
    void entradasDispensamToken() throws Exception {
        for (String acao : java.util.List.of("registrar", "entrar", "renovar", "sair")) {
            var resposta = chamar("POST", "/api/plataforma/v1/auth/" + acao, null, "{}",
                    "X-Usuario-Id", "id-da-vitima");

            assertThat(resposta.statusCode()).as(acao).isEqualTo(200);
            assertThat(resposta.body())
                    .contains("caminho=/api/v1/auth/" + acao)
                    .contains("usuario=null");
        }
    }

    @Test
    @DisplayName("o resto da autenticação continua exigindo token")
    void restoDaAutenticacaoExigeToken() throws Exception {
        assertThat(get("/api/plataforma/v1/auth/eu", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/plataforma/v1/auth/entrar", null).statusCode())
                .as("só o POST é aberto").isEqualTo(401);
    }

    @Test
    @DisplayName("rotas internas não são alcançáveis pela borda, nem saindo do caminho")
    void internoForaDeAlcance() throws Exception {
        for (String tentativa : java.util.List.of(
                "/internal/usage/saldo",
                "/api/plataforma/../../internal/usage/saldo",
                "/api/plataforma/%2e%2e/%2e%2e/internal/usage/saldo",
                "/api/plataforma/..%2f..%2finternal/usage/saldo",
                "/api/plataforma//../internal/usage/saldo")) {
            var resposta = get(tentativa, tokenValido());

            assertThat(resposta.statusCode()).as(tentativa).isBetween(400, 499);
            assertThat(resposta.body()).as(tentativa).doesNotContain("metodo=");
        }
    }
}
