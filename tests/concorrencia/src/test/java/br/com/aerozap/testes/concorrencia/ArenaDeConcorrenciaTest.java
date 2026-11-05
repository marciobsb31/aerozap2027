package br.com.aerozap.testes.concorrencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testa o harness, e não o sistema.
 *
 * <p>Uma ferramenta de teste de concorrência que não executa as tarefas de verdade ao mesmo tempo
 * é pior do que nenhuma: ela devolve verde e dá a impressão de que a invariante está protegida.
 * Por isso aqui se prova que a arena cria disputa real — usando um contador propositalmente
 * desprotegido, que só perde incrementos se houver sobreposição.
 */
class ArenaDeConcorrenciaTest {

    private static final int TAREFAS = 50;

    @Test
    @DisplayName("executa todas as operações e devolve o resultado de cada uma")
    void executaTodas() {
        var resultado = ArenaDeConcorrencia.disparar(50, indice -> indice * 2);

        assertThat(resultado.totalDeSucessos()).isEqualTo(50);
        assertThat(resultado.totalDeFalhas()).isZero();
        assertThat(resultado.sucessos()).containsExactlyInAnyOrderElementsOf(
                java.util.stream.IntStream.range(0, 50).mapToObj(i -> i * 2).toList());
    }

    @Test
    @DisplayName("cria sobreposição real: várias tarefas ficam dentro da seção ao mesmo tempo")
    void criaSobreposicaoReal() {
        // Este é o teste do teste. A primeira versão dele inferia a sobreposição de um contador
        // desprotegido perder incrementos — e isso depende do escalonador e do JIT, então ela
        // falhava em algumas máquinas e passava em outras. Teste instável é defeito, inclusive
        // quando o teste é meu. Agora a sobreposição é MEDIDA: cada tarefa registra quantas
        // estavam dentro da seção junto com ela.
        var dentro = new AtomicInteger();
        var maximoSimultaneo = new AtomicInteger();

        ArenaDeConcorrencia.disparar(TAREFAS, indice -> {
            int simultaneas = dentro.incrementAndGet();
            maximoSimultaneo.accumulateAndGet(simultaneas, Math::max);
            try {
                // Segura a tarefa dentro da seção enquanto as outras chegam. É a única pausa
                // legítima num teste de concorrência: a pausa É o objeto da medição.
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } finally {
                dentro.decrementAndGet();
            }
            return simultaneas;
        });

        assertThat(maximoSimultaneo.get())
                .as("a arena precisa sobrepor as tarefas; sem sobreposição não há teste de corrida")
                .isGreaterThanOrEqualTo(TAREFAS);
    }

    @Test
    @DisplayName("com exclusão mútua, nenhum incremento se perde")
    void comExclusaoMutuaNadaSePerde() {
        var trava = new ReentrantLock();
        var contador = new AtomicInteger();

        ArenaDeConcorrencia.disparar(200, indice -> {
            trava.lock();
            try {
                contador.incrementAndGet();
            } finally {
                trava.unlock();
            }
            return indice;
        });

        assertThat(contador.get()).isEqualTo(200);
    }

    @Test
    @DisplayName("separa sucesso de falha e identifica o tipo do erro")
    void separaSucessoDeFalha() {
        var resultado = ArenaDeConcorrencia.disparar(10, indice -> {
            if (indice % 2 == 0) {
                throw new IllegalStateException("recusado " + indice);
            }
            return indice;
        });

        assertThat(resultado.totalDeSucessos()).isEqualTo(5);
        assertThat(resultado.totalDeFalhas()).isEqualTo(5);
        assertThat(resultado.falhasDoTipo(IllegalStateException.class)).isEqualTo(5);
        assertThat(resultado.resumo()).contains("5 sucesso(s)", "IllegalStateException");
    }

    @Test
    @DisplayName("repetir diz em qual rodada a invariante quebrou")
    void repetirApontaARodada() {
        var rodadas = new AtomicInteger();

        assertThatThrownBy(() -> ArenaDeConcorrencia.repetir(5, 4,
                indice -> indice,
                resultado -> {
                    if (rodadas.incrementAndGet() == 3) {
                        assertThat(resultado.totalDeSucessos()).isEqualTo(99);
                    }
                }))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("rodada 3 de 5");
    }

    @Test
    @DisplayName("recusa arena com menos de duas operações")
    void recusaArenaDeUm() {
        assertThatThrownBy(() -> ArenaDeConcorrencia.disparar(1, indice -> indice))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
