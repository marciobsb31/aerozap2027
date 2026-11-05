package br.com.aerozap.apigateway.rota;

import br.com.aerozap.foundation.web.CorrelationIds;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Encaminha o que chega em {@code /api/**} para o serviço da rota correspondente.
 *
 * <p>Repassa método, caminho, parâmetros, cabeçalhos e corpo, acrescenta o identificador de
 * correlação e devolve a resposta como veio. Não interpreta o conteúdo: o gateway não conhece os
 * contratos dos serviços, e é isso que permite mudar um serviço sem mexer na borda.
 *
 * <p>A única identidade que os serviços recebem é a do token validado aqui. Um {@code X-Usuario-Id}
 * vindo do cliente é descartado: repassá-lo deixaria qualquer pessoa ler os dados de outra (RF-007).
 */
@RestController
public class EncaminhadorDeRequisicao {

    private static final Logger log = LoggerFactory.getLogger(EncaminhadorDeRequisicao.class);

    /** Cabeçalhos ponto a ponto: pertencem a uma conexão e não devem ser repassados (RFC 9110). */
    private static final Set<String> CABECALHOS_DA_CONEXAO = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization", "te",
            "trailer", "transfer-encoding", "upgrade", "host", "content-length", "expect");

    /** Quem está pedindo, para os serviços. Só a borda escreve este cabeçalho. */
    private static final String CABECALHO_DO_USUARIO = "X-Usuario-Id";

    private final RestClient cliente;
    private final PropriedadesDeRotas rotas;

    public EncaminhadorDeRequisicao(RestClient clienteDeEncaminhamento, PropriedadesDeRotas rotas) {
        this.cliente = clienteDeEncaminhamento;
        this.rotas = rotas;
    }

    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> encaminhar(HttpServletRequest requisicao,
                                             @RequestBody(required = false) byte[] corpo) {
        String caminho = requisicao.getRequestURI();
        PropriedadesDeRotas.Rota rota = rotas.paraCaminho(caminho);
        if (rota == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhuma rota configurada para este caminho.");
        }

        // O prefixo da rota é da borda, não do serviço: ele sai e o resto do caminho vai para o
        // destino. Assim o endereço público (/api/plataforma/v1/...) não amarra o caminho interno.
        String restante = caminho.substring(rota.prefixo().length());
        URI destino = UriComponentsBuilder.fromUriString(rota.destino() + restante)
                .query(requisicao.getQueryString())
                .build(true)
                .toUri();

        try {
            RestClient.RequestBodySpec chamada = cliente
                    .method(HttpMethod.valueOf(requisicao.getMethod()))
                    .uri(destino)
                    .headers(cabecalhos -> copiarCabecalhos(requisicao, cabecalhos));
            if (corpo != null && corpo.length > 0) {
                chamada = chamada.body(corpo);
            }
            ResponseEntity<byte[]> resposta = chamada.retrieve()
                    // O gateway repassa o erro do serviço como ele é; não o traduz nem o esconde.
                    .onStatus(status -> true, (pedido, recebida) -> { })
                    .toEntity(byte[].class);

            return ResponseEntity.status(resposta.getStatusCode())
                    .headers(limpar(resposta.getHeaders()))
                    .body(resposta.getBody());
        } catch (ResourceAccessException e) {
            log.warn("Serviço {} não respondeu a tempo: {}", rota.destino(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                    "O serviço de destino não respondeu a tempo.");
        }
    }

    private void copiarCabecalhos(HttpServletRequest requisicao, HttpHeaders destino) {
        for (String nome : Collections.list(requisicao.getHeaderNames())) {
            if (CABECALHOS_DA_CONEXAO.contains(nome.toLowerCase(java.util.Locale.ROOT))
                    || CABECALHO_DO_USUARIO.equalsIgnoreCase(nome)) {
                continue;
            }
            destino.addAll(nome, Collections.list(requisicao.getHeaders(nome)));
        }
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            destino.set(CABECALHO_DO_USUARIO, token.getToken().getSubject());
        }
        destino.set(CorrelationIds.HEADER, CorrelationIds.current());
        destino.set("X-Forwarded-For", requisicao.getRemoteAddr());
        destino.set("X-Forwarded-Proto", requisicao.getScheme());
        destino.set("X-Forwarded-Host", requisicao.getServerName());
    }

    private HttpHeaders limpar(HttpHeaders recebidos) {
        HttpHeaders saida = new HttpHeaders();
        recebidos.forEach((nome, valores) -> {
            if (!CABECALHOS_DA_CONEXAO.contains(nome.toLowerCase(java.util.Locale.ROOT))) {
                saida.addAll(nome, List.copyOf(valores));
            }
        });
        return saida;
    }
}
