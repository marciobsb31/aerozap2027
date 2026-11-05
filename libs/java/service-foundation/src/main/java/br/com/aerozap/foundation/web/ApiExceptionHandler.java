package br.com.aerozap.foundation.web;

import br.com.aerozap.contracts.ProblemTypes;
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

    /**
     * Erro que o domínio previu: o tipo, o título e a mensagem são os que o serviço escreveu.
     * Fica antes do tratador genérico porque o Spring escolhe o tratador mais específico.
     */
    @ExceptionHandler(ProblemaDeNegocio.class)
    public ProblemDetail handleNegocio(ProblemaDeNegocio ex) {
        // Nível de aviso, não de erro: isto é o sistema funcionando, não quebrando. Registrar como
        // erro encheria o alerta de ruído e esconderia a falha de verdade.
        log.warn("Problema de negócio: {} ({})", ex.titulo(), ex.tipo());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        problem.setType(URI.create(ex.tipo()));
        problem.setTitle(ex.titulo());
        problem.setProperty("correlationId", CorrelationIds.current());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro não tratado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno. Informe o código de correlação ao suporte.");
        problem.setType(URI.create(ProblemTypes.ERRO_INTERNO));
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
            // O Spring preenche `type` com "about:blank", que não diz nada a quem consome. Como o
            // catálogo de tipos é contrato (ProblemTypes), o erro gerado pelo próprio MVC recebe
            // aqui o tipo correspondente ao status — senão metade dos erros da API teria tipo e a
            // outra metade não, justamente a metade que o cliente não controla.
            if (problem.getType() == null || "about:blank".equals(problem.getType().toString())) {
                problem.setType(URI.create(tipoPara(statusCode)));
            }
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static String tipoPara(HttpStatusCode status) {
        return switch (status.value()) {
            case 400, 422 -> ProblemTypes.VALIDACAO;
            case 401 -> ProblemTypes.NAO_AUTENTICADO;
            case 403 -> ProblemTypes.ACESSO_NEGADO;
            case 404, 405, 406 -> ProblemTypes.NAO_ENCONTRADO;
            case 409 -> ProblemTypes.CONFLITO;
            case 429 -> ProblemTypes.LIMITE_DE_TAXA;
            case 504 -> ProblemTypes.TEMPO_ESGOTADO;
            default -> ProblemTypes.ERRO_INTERNO;
        };
    }
}
