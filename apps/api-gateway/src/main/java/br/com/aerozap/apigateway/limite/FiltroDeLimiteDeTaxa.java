package br.com.aerozap.apigateway.limite;

import br.com.aerozap.apigateway.seguranca.ProblemaHttp;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Aplica o limite de taxa antes da autenticação, para que um ataque com tokens inválidos também
 * seja contido. A origem é o endereço de rede do cliente.
 *
 * <p>Atrás de balanceador, o endereço real só chega se os cabeçalhos encaminhados forem
 * confiáveis — por isso a leitura de {@code X-Forwarded-For} não é feita aqui, e sim pelo próprio
 * servidor quando {@code server.forward-headers-strategy} estiver configurado com um proxy de
 * confiança. Ver docs/architecture/api-gateway.md.
 */
public class FiltroDeLimiteDeTaxa extends OncePerRequestFilter {

    private static final AntPathMatcher CAMINHOS = new AntPathMatcher();

    private final LimitadorDeTaxa limitador;
    private final PropriedadesDeLimite propriedades;
    private final ObjectMapper json;

    public FiltroDeLimiteDeTaxa(LimitadorDeTaxa limitador, PropriedadesDeLimite propriedades, ObjectMapper json) {
        this.limitador = limitador;
        this.propriedades = propriedades;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        if (!propriedades.habilitado()) {
            return true;
        }
        String caminho = requisicao.getRequestURI();
        return propriedades.caminhosLiberados().stream().anyMatch(padrao -> CAMINHOS.match(padrao, caminho));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        LimitadorDeTaxa.Resultado resultado = limitador.verificar(origem(requisicao));

        resposta.setHeader("X-RateLimit-Limit", String.valueOf(resultado.limite()));
        resposta.setHeader("X-RateLimit-Remaining", String.valueOf(resultado.restantes()));
        resposta.setHeader("X-RateLimit-Reset", String.valueOf(resultado.reiniciaEmSegundos()));

        if (!resultado.permitido()) {
            resposta.setHeader("Retry-After", String.valueOf(resultado.reiniciaEmSegundos()));
            ProblemaHttp.escrever(resposta, json, HttpStatus.TOO_MANY_REQUESTS, "rate-limit",
                    "Muitas requisições",
                    "Limite de " + resultado.limite() + " requisições por janela excedido. "
                            + "Tente novamente em " + resultado.reiniciaEmSegundos() + " segundos.");
            return;
        }

        cadeia.doFilter(requisicao, resposta);
    }

    private String origem(HttpServletRequest requisicao) {
        String endereco = requisicao.getRemoteAddr();
        return endereco != null ? endereco : "desconhecida";
    }
}
