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
    @DisplayName("cria disputa real: um contador sem proteção perde incrementos")
    void criaDisputaReal() {
        // Este é o teste do teste. Se a arena serializasse as tarefas, o contador chegaria a 2000
        // em todas as rodadas e a prova de simultaneidade não existiria.
        boolean perdeuAlgumIncremento = false;
        for (int rodada = 0; rodada < 20 && !perdeuAlgumIncremento; rodada++) {
            var contador = new int[1];
            ArenaDeConcorrencia.disparar(200, indice -> {
                for (int i = 0; i < 1_000; i++) {
                    contador[0] = contador[0] + 1;
                }
                return indice;
            });
            perdeuAlgumIncremento = contador[0] < 200_000;
        }
        assertThat(perdeuAlgumIncremento)
                .as("a arena precisa sobrepor as tarefas; sem sobreposição não há teste de corrida")
                .isTrue();
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
