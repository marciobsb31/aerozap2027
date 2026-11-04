package br.com.aerozap.apigateway.seguranca;

import br.com.aerozap.foundation.web.CorrelationIds;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/**
 * Escreve erros no formato Problem Details (RFC 9457) direto na resposta.
 *
 * <p>Necessário porque os erros de autenticação e de limite de taxa acontecem em filtros, antes
 * do tratamento de exceções do Spring MVC. O formato é o mesmo do resto da plataforma, para o
 * cliente não precisar de dois tratamentos.
 */
public final class ProblemaHttp {

    private ProblemaHttp() {
    }

    public static void escrever(HttpServletResponse resposta, ObjectMapper json, HttpStatus status,
                                String tipo, String titulo, String detalhe) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setType(URI.create("urn:aerozap:problem:" + tipo));
        problema.setTitle(titulo);
        problema.setProperty("correlationId", CorrelationIds.current());

        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        json.writeValue(resposta.getOutputStream(), problema);
    }
}
