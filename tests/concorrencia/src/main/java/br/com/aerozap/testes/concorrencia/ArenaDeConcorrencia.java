package br.com.aerozap.testes.concorrencia;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * Dispara N operações no mesmo instante e devolve o que cada uma produziu.
 *
 * <p>O ponto é a largada comum. Um laço que cria N tarefas e sai executando quase nunca coloca duas
 * delas dentro da seção crítica ao mesmo tempo: a primeira termina antes de a última nascer, o
 * teste passa, e a corrida aparece em produção. Aqui cada tarefa avisa que está pronta, espera a
 * largada e só então executa — é isso que transforma "N chamadas" em "N chamadas simultâneas".
 *
 * <p>Nenhuma asserção mora aqui. O harness coleta fatos — quantas deram certo, quantas falharam e
 * com qual erro — e quem escreve o teste declara a invariante. Misturar as duas coisas produziria
 * um harness que só serve para um cenário.
 *
 * <p>Thread virtual por tarefa (Java 21+): mil operações simultâneas não custam mil threads do
 * sistema operacional, e o teste de pico deixa de ser um exercício de configurar pool.
 */
public final class ArenaDeConcorrencia {

    /** Tempo máximo para todas as tarefas terminarem. Travar é defeito, não lentidão. */
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(60);

    private ArenaDeConcorrencia() {
    }

    /**
     * O que N operações simultâneas produziram.
     *
     * @param sucessos valor devolvido por cada operação que terminou sem exceção, em ordem de
     *                 chegada (que é indeterminada de propósito: depender dela seria depender do
     *                 escalonador)
     * @param falhas   a exceção de cada operação que falhou
     * @param duracao  do sinal de largada até a última tarefa terminar
     */
    public record Resultado<T>(List<T> sucessos, List<Throwable> falhas, Duration duracao) {

        public int totalDeSucessos() {
            return sucessos.size();
        }

        public int totalDeFalhas() {
            return falhas.size();
        }

        public long sucessosQue(Predicate<T> condicao) {
            return sucessos.stream().filter(condicao).count();
        }

        public long falhasDoTipo(Class<? extends Throwable> tipo) {
            return falhas.stream().filter(e -> tipo.isInstance(causaRaiz(e))).count();
        }

        /** Resumo legível para a mensagem de falha do teste: sem isso, depurar vira adivinhação. */
        public String resumo() {
            StringBuilder texto = new StringBuilder()
                    .append(totalDeSucessos()).append(" sucesso(s), ")
                    .append(totalDeFalhas()).append(" falha(s) em ").append(duracao.toMillis())
                    .append(" ms");
            falhas.stream()
                    .map(e -> causaRaiz(e).getClass().getSimpleName() + ": " + causaRaiz(e).getMessage())
                    .distinct()
                    .forEach(mensagem -> texto.append("\n  - ").append(mensagem));
            return texto.toString();
        }

        private static Throwable causaRaiz(Throwable erro) {
            Throwable atual = erro;
            while (atual.getCause() != null && atual.getCause() != atual) {
                atual = atual.getCause();
            }
            return atual;
        }
    }

    /**
     * Executa {@code quantidade} vezes a mesma operação, todas liberadas no mesmo instante.
     *
     * @param tarefa recebe o índice da operação (0 a quantidade-1) e devolve o resultado dela
     */
    public static <T> Resultado<T> disparar(int quantidade, IntFunction<T> tarefa) {
        if (quantidade < 2) {
            throw new IllegalArgumentException(
                    "Uma arena com menos de duas operações não testa concorrência nenhuma.");
        }

        var sucessos = new ConcurrentLinkedQueue<T>();
        var falhas = new ConcurrentLinkedQueue<Throwable>();
        var prontas = new CountDownLatch(quantidade);
        var largada = new CountDownLatch(1);
        var terminadas = new CountDownLatch(quantidade);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int indice = 0; indice < quantidade; indice++) {
                final int meuIndice = indice;
                executor.submit(() -> {
                    prontas.countDown();
                    try {
                        largada.await();
                        sucessos.add(tarefa.apply(meuIndice));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        falhas.add(e);
                    } catch (Throwable e) {
                        falhas.add(e);
                    } finally {
                        terminadas.countDown();
                    }
                });
            }

            esperar(prontas, "as tarefas chegarem à linha de largada");
            long inicio = System.nanoTime();
            largada.countDown();
            esperar(terminadas, "as tarefas terminarem");
            Duration duracao = Duration.ofNanos(System.nanoTime() - inicio);

            return new Resultado<>(List.copyOf(new ArrayList<>(sucessos)),
                    List.copyOf(new ArrayList<>(falhas)), duracao);
        }
    }

    /**
     * Repete a arena inteira algumas vezes.
     *
     * <p>Corrida é probabilística: a que escapa aparece uma vez a cada vinte execuções, e um teste
     * que roda uma vez só diz "passou" quase sempre. Repetir é o que transforma sorte em evidência.
     * O preço é tempo de CI, então o número de repetições é escolha de quem escreve o teste.
     */
    public static <T> void repetir(int vezes, int quantidade, IntFunction<T> tarefa,
                                   java.util.function.Consumer<Resultado<T>> invariante) {
        repetir(vezes, quantidade, () -> { }, tarefa, invariante);
    }

    /**
     * Como {@link #repetir(int, int, IntFunction, java.util.function.Consumer)}, com preparação
     * antes de cada rodada — repor o saldo, limpar a tabela, recriar o estado disputado.
     */
    public static <T> void repetir(int vezes, int quantidade, Runnable preparacao,
                                   IntFunction<T> tarefa,
                                   java.util.function.Consumer<Resultado<T>> invariante) {
        for (int rodada = 1; rodada <= vezes; rodada++) {
            preparacao.run();
            Resultado<T> resultado = disparar(quantidade, tarefa);
            try {
                invariante.accept(resultado);
            } catch (AssertionError falha) {
                throw new AssertionError("Invariante quebrou na rodada " + rodada + " de " + vezes
                        + ". " + resultado.resumo(), falha);
            }
        }
    }

    private static void esperar(CountDownLatch porta, String oQue) {
        try {
            if (!porta.await(ESPERA_MAXIMA.toSeconds(), TimeUnit.SECONDS)) {
                throw new AssertionError("Esperei " + ESPERA_MAXIMA.toSeconds() + "s por " + oQue
                        + " e " + porta.getCount() + " não respondeu(ram). Travamento é defeito.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrompido esperando " + oQue, e);
        }
    }
}
