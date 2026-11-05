package br.com.aerozap.conversationai.llm;

import java.time.Duration;
import java.util.Objects;

/**
 * Pedido a um provedor de modelo de linguagem.
 *
 * <p>A instrução do sistema e o texto da pessoa são campos separados, e isso é desenho de
 * segurança, não arrumação: concatenar os dois numa string só é o que torna injeção de prompt
 * trivial. Separados, o provedor pode mandá-los em papéis diferentes, e fica explícito no tipo
 * qual deles é conteúdo não confiável.
 *
 * @param versaoDoPrompt identificação da versão da instrução, registrada em toda interação (RF-049)
 * @param instrucao instrução do sistema, escrita por nós
 * @param entradaDoUsuario texto da pessoa; conteúdo não confiável
 * @param tempoLimite prazo máximo de resposta
 */
public record PedidoAoLlm(String versaoDoPrompt, String instrucao, String entradaDoUsuario,
                          Duration tempoLimite) {

    public PedidoAoLlm {
        Objects.requireNonNull(versaoDoPrompt, "versaoDoPrompt");
        Objects.requireNonNull(instrucao, "instrucao");
        Objects.requireNonNull(entradaDoUsuario, "entradaDoUsuario");
        Objects.requireNonNull(tempoLimite, "tempoLimite");
        if (tempoLimite.isZero() || tempoLimite.isNegative()) {
            throw new IllegalArgumentException("tempoLimite precisa ser positivo");
        }
    }
}
