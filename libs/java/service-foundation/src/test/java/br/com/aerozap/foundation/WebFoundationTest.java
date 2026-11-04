package br.com.aerozap.foundation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.aerozap.foundation.web.ApiExceptionHandler;
import br.com.aerozap.foundation.web.ApiResponse;
import br.com.aerozap.foundation.web.CorrelationIdFilter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

class WebFoundationTest {

    @RestController
    static class SampleController {
        @GetMapping("/ok")
        ApiResponse<Map<String, String>> ok() {
            return ApiResponse.of(Map.of("status", "ok"));
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("segredo interno que não pode vazar");
        }

        @GetMapping("/ausente")
        String ausente() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Não existe.");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    @Test
    void devolveOCorrelationIdRecebido() throws Exception {
        mvc.perform(get("/ok").header("X-Correlation-Id", "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "abc-123"))
                .andExpect(jsonPath("$.data.status").value("ok"))
                .andExpect(jsonPath("$.meta.correlationId").value("abc-123"));
    }

    @Test
    void geraCorrelationIdQuandoAusenteOuInseguro() throws Exception {
        mvc.perform(get("/ok").header("X-Correlation-Id", "inseguro\nvalor"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void erroInternoViraProblemDetailsSemVazarDetalhes() throws Exception {
        mvc.perform(get("/boom").header("X-Correlation-Id", "corr-9"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.correlationId").value("corr-9"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("segredo"))));
    }

    /**
     * Erros que o próprio Spring MVC monta (404, 405, 400) passam por outro caminho, no qual o
     * corpo só é criado depois. Sem este teste, a correlação sumia justamente nas respostas de
     * erro mais comuns — e é nelas que o cliente mais precisa do código para pedir suporte.
     */
    @Test
    void erroGeradoPorExcecaoTambemCarregaACorrelacao() throws Exception {
        mvc.perform(get("/ausente").header("X-Correlation-Id", "corr-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.correlationId").value("corr-404"));
    }
}
