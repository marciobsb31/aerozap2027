package br.com.aerozap.identity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/** Base dos testes de integração da identidade: PostgreSQL e RabbitMQ reais. Exige Docker. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIdentityIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.5-alpine");
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.1.0-management-alpine");

    static {
        POSTGRES.start();
        RABBIT.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName() + "?currentSchema=identity");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("aerozap.identidade.token.segredo",
                () -> "segredo-de-teste-com-pelo-menos-32-bytes!!");
    }

    @Value("${local.server.port}")
    int porta;

    /** Cliente HTTP do próprio JDK, como no resto dos testes de integração do projeto. */
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();
    private static final tools.jackson.databind.ObjectMapper JSON =
            new tools.jackson.databind.ObjectMapper();

    protected Resposta postar(String caminho, Map<String, ?> corpo) {
        return enviar(HttpRequest.newBuilder(uri(caminho))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(corpo))));
    }

    protected Resposta obter(String caminho, String... cabecalhos) {
        var construtor = HttpRequest.newBuilder(uri(caminho)).GET();
        if (cabecalhos.length > 0) {
            construtor.headers(cabecalhos);
        }
        return enviar(construtor);
    }

    private URI uri(String caminho) {
        return URI.create("http://localhost:" + porta + caminho);
    }

    private static Resposta enviar(HttpRequest.Builder construtor) {
        try {
            HttpResponse<String> resposta =
                    CLIENTE.send(construtor.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, Object> corpo = resposta.body() == null || resposta.body().isBlank()
                    ? Map.of()
                    : JSON.readValue(resposta.body(), Map.class);
            return new Resposta(resposta.statusCode(), corpo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Status e corpo já em mapa: os testes falam de conteúdo, não de desserialização. */
    protected record Resposta(int status, Map<String, Object> corpo) {

        @SuppressWarnings("unchecked")
        public Map<String, Object> dados() {
            return (Map<String, Object>) corpo.get("data");
        }

        public String texto(String campo) {
            return (String) dados().get(campo);
        }
    }
}
