package br.com.aerozap.apigateway;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

/**
 * Base dos testes do gateway: Redis real (Testcontainers, mesma imagem do Compose) e um segredo
 * de assinatura de teste. Cada teste define o que precisa do limite de taxa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractGatewayIntegrationTest {

    static final String SEGREDO = "segredo-de-teste-apenas-local-com-32+";
    static final String EMISSOR = "https://aerozap.local/identidade";
    static final String PUBLICO = "aerozap-api";

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4.2-alpine")
            .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @Value("${local.server.port}")
    protected int porta;

    @DynamicPropertySource
    static void propriedadesBase(DynamicPropertyRegistry registro) {
        registro.add("spring.data.redis.host", REDIS::getHost);
        registro.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registro.add("spring.data.redis.password", () -> "");
        registro.add("aerozap.seguranca.jwt.segredo", () -> SEGREDO);
        registro.add("aerozap.seguranca.jwt.emissor", () -> EMISSOR);
        registro.add("aerozap.seguranca.jwt.publico", () -> PUBLICO);
    }

    /** Token válido para os testes: assinado com o segredo de teste, emissor e público corretos. */
    protected static String tokenValido() {
        return token(SEGREDO, EMISSOR, PUBLICO, Duration.ofMinutes(5));
    }

    protected static String token(String segredo, String emissor, String publico, Duration validade) {
        try {
            JWTClaimsSet dados = new JWTClaimsSet.Builder()
                    .subject("usuario-de-teste")
                    .issuer(emissor)
                    .audience(List.of(publico))
                    .issueTime(Date.from(Instant.now().minusSeconds(5)))
                    .expirationTime(Date.from(Instant.now().plus(validade)))
                    .build();
            SignedJWT assinado = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), dados);
            assinado.sign(new MACSigner(segredo.getBytes(StandardCharsets.UTF_8)));
            return assinado.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("não foi possível gerar o token de teste", e);
        }
    }

    protected HttpResponse<String> chamar(String metodo, String caminho, String token, String corpo,
                                          String... cabecalhos) throws Exception {
        HttpRequest.Builder construtor = HttpRequest
                .newBuilder(URI.create("http://localhost:" + porta + caminho))
                .method(metodo, corpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corpo));
        if (token != null) {
            construtor.header("Authorization", "Bearer " + token);
        }
        if (cabecalhos.length > 0) {
            construtor.headers(cabecalhos);
        }
        return HttpClient.newHttpClient().send(construtor.build(), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> get(String caminho, String token, String... cabecalhos) throws Exception {
        return chamar("GET", caminho, token, null, cabecalhos);
    }
}
