package br.com.aerozap.platformcore;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/** Critério de aceite: o serviço sobe e responde ao health check; contrato OpenAPI e envelope padrão. */
class ServiceEndpointsIntegrationTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    int port;

    private HttpResponse<String> get(String path, String... headers) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (headers.length > 0) {
            builder.headers(headers);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthCheckResponde() throws Exception {
        var response = get("/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void probesDeLivenessEReadiness() throws Exception {
        assertThat(get("/actuator/health/liveness").statusCode()).isEqualTo(200);
        assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(200);
    }

    @Test
    void pingUsaOEnvelopePadraoEPropagaACorrelacao() throws Exception {
        var response = get("/api/v1/system/ping", "X-Correlation-Id", "teste-123");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Correlation-Id")).contains("teste-123");
        assertThat(response.body()).contains("\"data\"").contains("\"meta\"").contains("teste-123");
    }

    @Test
    void contratoOpenApiEstaPublicado() throws Exception {
        var response = get("/v3/api-docs");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("/api/v1/system/ping");
    }
}
