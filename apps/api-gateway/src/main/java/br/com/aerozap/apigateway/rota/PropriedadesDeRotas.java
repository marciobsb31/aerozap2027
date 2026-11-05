package br.com.aerozap.apigateway.rota;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tabela de rotas da borda: cada prefixo de caminho vai para um serviço.
 *
 * <p>Roteamento é só isto — prefixo e destino. Nada de condição por usuário, plano ou conteúdo:
 * decisão de negócio no gateway é regra de negócio escondida na borda, e isso a AZ-015 proíbe.
 *
 * @param rotas pares prefixo/destino, avaliados do prefixo mais específico para o mais genérico
 */
@ConfigurationProperties("aerozap.gateway")
public record PropriedadesDeRotas(@DefaultValue List<Rota> rotas) {

    /**
     * <p>O prefixo é removido antes do encaminhamento, e o que sobra é acrescentado ao destino.
     * Exemplo: prefixo {@code /api/plataforma}, destino {@code http://localhost:8081/api};
     * {@code /api/plataforma/v1/ping} chega ao serviço como {@code /api/v1/ping}.
     *
     * @param prefixo início do caminho na borda, por exemplo {@code /api/plataforma}
     * @param destino base no serviço, por exemplo {@code http://localhost:8081/api}
     */
    public record Rota(String prefixo, String destino) {

        public Rota {
            if (prefixo == null || !prefixo.startsWith("/api/")) {
                throw new IllegalArgumentException("prefixo de rota precisa começar com /api/: " + prefixo);
            }
            if (destino == null || !(destino.startsWith("http://") || destino.startsWith("https://"))) {
                throw new IllegalArgumentException("destino de rota precisa ser http(s): " + destino);
            }
            if (destino.endsWith("/")) {
                throw new IllegalArgumentException("destino de rota não deve terminar com /: " + destino);
            }
        }
    }

    /** Rota cujo prefixo casa com o caminho; a mais específica ganha. */
    public Rota paraCaminho(String caminho) {
        Rota escolhida = null;
        for (Rota rota : rotas) {
            boolean casa = caminho.equals(rota.prefixo()) || caminho.startsWith(rota.prefixo() + "/");
            if (casa && (escolhida == null || rota.prefixo().length() > escolhida.prefixo().length())) {
                escolhida = rota;
            }
        }
        return escolhida;
    }
}
