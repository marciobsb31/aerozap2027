package br.com.aerozap.conversationai.interpretacao;

import java.time.LocalDate;
import java.util.Objects;

/**
 * O que o interpretador precisa saber além da frase.
 *
 * <p>A data de hoje entra como dado, não vem do relógio do sistema: "sexta que vem" só tem sentido
 * em relação a um dia, e um interpretador que lê o relógio por dentro não pode ser testado.
 *
 * <p>A intenção anterior é o que permite refinamento ("na verdade quero voltar dia 25"). O
 * interpretador v0 a ignora — regras não sabem o que fazer com ela —, e é exatamente isso que o
 * conjunto dourado mede como lacuna para o motor de linguagem do M4.
 *
 * @param hoje data de referência para expressões relativas
 * @param moedaPadrao moeda quando a pessoa não diz
 * @param anterior o que já tinha sido entendido na conversa; nulo na primeira mensagem
 */
public record ContextoDaInterpretacao(LocalDate hoje, String moedaPadrao, IntencaoParcial anterior) {

    public ContextoDaInterpretacao {
        Objects.requireNonNull(hoje, "hoje");
        moedaPadrao = moedaPadrao == null ? "BRL" : moedaPadrao;
    }

    public static ContextoDaInterpretacao em(LocalDate hoje) {
        return new ContextoDaInterpretacao(hoje, null, null);
    }

    public ContextoDaInterpretacao continuando(IntencaoParcial anterior) {
        return new ContextoDaInterpretacao(hoje, moedaPadrao, anterior);
    }
}
