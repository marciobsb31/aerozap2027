package br.com.aerozap.foundation.web;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Erros no formato Problem Details (RFC 9457), sempre com o correlationId.
 * Nunca expõe stack trace nem detalhes internos ao cliente.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro não tratado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno. Informe o código de correlação ao suporte.");
        problem.setType(URI.create("urn:aerozap:problem:internal-error"));
        problem.setTitle("Erro interno");
        problem.setProperty("correlationId", CorrelationIds.current());
        return problem;
    }

    /**
     * Único ponto por onde passam todos os erros tratados pelo Spring MVC, já com o corpo
     * montado. Em {@code handleExceptionInternal} o corpo ainda pode vir nulo — é a classe base
     * que cria o ProblemDetail depois —, e aí a correlação não chegava ao cliente nas respostas
     * geradas por exceção (404, 405, 400 e as demais).
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            problem.setProperty("correlationId", CorrelationIds.current());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }
}
