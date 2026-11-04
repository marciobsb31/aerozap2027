package br.com.aerozap.conversationai.llm;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registra tokens, latência e custo de cada interação com o modelo (RF-049).
 *
 * <p>Métrica por modelo e por versão de prompt, não um total só: é o que permite responder "o
 * prompt novo ficou mais caro?" sem adivinhação. O texto da pessoa nunca entra no registro.
 */
public class RegistroDeUsoDeLlm {

    private static final Logger log = LoggerFactory.getLogger(RegistroDeUsoDeLlm.class);

    private final MeterRegistry metricas;

    public RegistroDeUsoDeLlm(MeterRegistry metricas) {
        this.metricas = metricas;
    }

    public void registrar(UsoDeLlm uso) {
        Tags etiquetas = Tags.of("modelo", uso.modelo(), "versao_do_prompt", uso.versaoDoPrompt());

        metricas.counter("aerozap.llm.tokens", etiquetas.and("direcao", "entrada"))
                .increment(uso.tokensDeEntrada());
        metricas.counter("aerozap.llm.tokens", etiquetas.and("direcao", "saida"))
                .increment(uso.tokensDeSaida());
        metricas.counter("aerozap.llm.custo", etiquetas.and("moeda", uso.moeda()))
                .increment(uso.custo().doubleValue());
        metricas.timer("aerozap.llm.latencia", etiquetas)
                .record(uso.latencia().toMillis(), TimeUnit.MILLISECONDS);

        log.info("Interação com o modelo: modelo={} prompt={} tokens={} latenciaMs={} custo={} {}",
                uso.modelo(), uso.versaoDoPrompt(), uso.tokensTotais(), uso.latencia().toMillis(),
                uso.custo(), uso.moeda());
    }
}
