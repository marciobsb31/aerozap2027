package br.com.aerozap.conversationai.interpretacao;

/**
 * Transforma o que a pessoa escreveu em pesquisa, pergunta ou recusa.
 *
 * <p>A porta existe para que a troca do motor — regras hoje, modelo de linguagem no M4 — não
 * mexa em quem usa. O conjunto dourado mede os dois contra o mesmo gabarito, e é isso que torna
 * a troca uma decisão com número, não com impressão.
 */
public interface Interpretador {

    /** Identificação do motor, para registro e comparação (por exemplo {@code deterministico-v0}). */
    String nome();

    ResultadoDaInterpretacao interpretar(Enunciado enunciado, ContextoDaInterpretacao contexto);
}
