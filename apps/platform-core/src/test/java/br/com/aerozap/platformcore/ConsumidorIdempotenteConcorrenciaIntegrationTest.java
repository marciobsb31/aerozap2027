package br.com.aerozap.platformcore;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.foundation.outbox.IdempotentConsumer;
import br.com.aerozap.testes.concorrencia.ArenaDeConcorrencia;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Entrega repetida da mesma mensagem: o efeito acontece uma vez só.
 *
 * <p>RabbitMQ entrega ao menos uma vez — reentrega é o comportamento normal, não a exceção. O que
 * decide se isso vira cobrança dobrada ou não é o consumidor idempotente, e o teste sequencial
 * dele (chamar duas vezes em seguida) passa mesmo quando a proteção é frágil. A prova precisa ser
 * com as N entregas caindo juntas, como acontece quando o broker redistribui depois de um
 * reinício.
 */
class ConsumidorIdempotenteConcorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final int CONCORRENTES = 24;
    private static final int RODADAS = 3;

    @Autowired
    private IdempotentConsumer consumidor;

    @Test
    @DisplayName("24 entregas simultâneas do mesmo evento: o efeito roda uma vez")
    void efeitoUnicoSobEntregaRepetida() {
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            UUID evento = UUID.randomUUID();
            var execucoes = new AtomicInteger();

            var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES,
                    indice -> consumidor.process(evento, "teste-concorrencia",
                            execucoes::incrementAndGet));

            assertThat(resultado.totalDeFalhas())
                    .as("perder a disputa não é erro: é devolver false. " + resultado.resumo())
                    .isZero();
            assertThat(resultado.sucessosQue(Boolean.TRUE::equals))
                    .as("rodada " + rodada + ": só um consumidor executa o efeito")
                    .isEqualTo(1);
            assertThat(execucoes.get())
                    .as("rodada " + rodada + ": o efeito em si roda uma vez")
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("consumidores diferentes do mesmo evento cada um executa uma vez")
    void cadaConsumidorTemSuaPropriaIdempotencia() {
        UUID evento = UUID.randomUUID();
        var execucoes = new AtomicInteger();

        var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES,
                indice -> consumidor.process(evento, "consumidor-" + (indice % 4),
                        execucoes::incrementAndGet));

        assertThat(resultado.totalDeFalhas()).as(resultado.resumo()).isZero();
        assertThat(resultado.sucessosQue(Boolean.TRUE::equals))
                .as("quatro consumidores distintos, quatro execuções")
                .isEqualTo(4);
        assertThat(execucoes.get()).isEqualTo(4);
    }
}
