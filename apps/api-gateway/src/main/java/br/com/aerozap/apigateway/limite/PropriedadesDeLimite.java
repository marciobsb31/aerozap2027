package br.com.aerozap.apigateway.limite;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Limite de taxa na borda, por origem da requisição.
 *
 * <p>É proteção de infraestrutura, não regra de negócio: serve para nenhuma origem derrubar a
 * plataforma. A franquia de consultas por plano é outra coisa, vive no serviço de uso
 * (ADR-0004) e não tem nada a ver com este número.
 *
 * @param habilitado liga ou desliga a verificação
 * @param requisicoes quantas requisições uma origem pode fazer por janela
 * @param janela tamanho da janela
 * @param caminhosLiberados caminhos que não contam para o limite
 */
@ConfigurationProperties("aerozap.limite-de-taxa")
public record PropriedadesDeLimite(
        @DefaultValue("true") boolean habilitado,
        @DefaultValue("120") int requisicoes,
        @DefaultValue("1m") Duration janela,
        @DefaultValue({"/actuator/health", "/actuator/info"}) List<String> caminhosLiberados) {

    public PropriedadesDeLimite {
        if (requisicoes < 1) {
            throw new IllegalArgumentException("aerozap.limite-de-taxa.requisicoes precisa ser pelo menos 1");
        }
        if (janela == null || janela.isZero() || janela.isNegative()) {
            throw new IllegalArgumentException("aerozap.limite-de-taxa.janela precisa ser positiva");
        }
    }

    long janelaEmSegundos() {
        return Math.max(1, janela.toSeconds());
    }
}
