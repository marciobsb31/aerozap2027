package br.com.aerozap.apigateway.limite;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Janela fixa no Redis: conta as requisições de uma origem no intervalo corrente.
 *
 * <p>O Redis é compartilhado entre instâncias do gateway, então o limite vale para o conjunto e
 * não por processo. A contagem e o vencimento acontecem num único script, para não existir
 * contador sem prazo de validade em caso de corrida.
 *
 * <p><strong>Falha em aberto, de propósito:</strong> se o Redis não responder, a requisição
 * passa. Perder o limite por alguns segundos é menos grave que derrubar a borda inteira.
 */
public class LimitadorDeTaxa {

    private static final Logger log = LoggerFactory.getLogger(LimitadorDeTaxa.class);
    private static final String PREFIXO = "aerozap:limite:";

    private final StringRedisTemplate redis;
    private final RedisScript<String> script;
    private final PropriedadesDeLimite propriedades;

    public LimitadorDeTaxa(StringRedisTemplate redis, RedisScript<String> script,
                           PropriedadesDeLimite propriedades) {
        this.redis = redis;
        this.script = script;
        this.propriedades = propriedades;
    }

    /** Resultado da verificação; {@code reiniciaEmSegundos} alimenta o cabeçalho Retry-After. */
    public record Resultado(boolean permitido, int limite, int restantes, long reiniciaEmSegundos) {

        static Resultado liberado(int limite, long janela) {
            return new Resultado(true, limite, limite, janela);
        }
    }

    public Resultado verificar(String origem) {
        long janela = propriedades.janelaEmSegundos();
        int limite = propriedades.requisicoes();
        long intervalo = System.currentTimeMillis() / (janela * 1000);
        String chave = PREFIXO + origem + ":" + intervalo;

        try {
            String retorno = redis.execute(script, List.of(chave),
                    String.valueOf(limite), String.valueOf(janela));
            return interpretar(retorno, limite, janela);
        } catch (RuntimeException e) {
            log.warn("Limite de taxa indisponível ({}); a requisição passou sem contagem.", e.getMessage());
            return Resultado.liberado(limite, janela);
        }
    }

    private Resultado interpretar(String retorno, int limite, long janela) {
        if (retorno == null) {
            return Resultado.liberado(limite, janela);
        }
        String[] partes = retorno.split(";");
        if (partes.length != 3) {
            log.warn("Resposta inesperada do limite de taxa: {}", retorno);
            return Resultado.liberado(limite, janela);
        }
        boolean permitido = "1".equals(partes[0]);
        int restantes = Integer.parseInt(partes[1]);
        long reinicia = Long.parseLong(partes[2]);
        return new Resultado(permitido, limite, restantes, reinicia);
    }
}
