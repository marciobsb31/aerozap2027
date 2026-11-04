package br.com.aerozap.conversationai.llm;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * O que custou uma interação com o modelo (RF-049).
 *
 * <p>Registrar isto desde o primeiro dia é o que impede a surpresa de fim de mês e o que permite
 * decidir entre modelos com número em vez de opinião. Custo de IA é risco declarado do projeto.
 *
 * @param modelo identificação do modelo
 * @param versaoDoPrompt versão da instrução usada
 * @param tokensDeEntrada tokens enviados
 * @param tokensDeSaida tokens recebidos
 * @param latencia tempo de resposta
 * @param custo custo estimado da chamada
 * @param moeda moeda do custo
 */
public record UsoDeLlm(
        String modelo,
        String versaoDoPrompt,
        int tokensDeEntrada,
        int tokensDeSaida,
        Duration latencia,
        BigDecimal custo,
        String moeda) {

    public UsoDeLlm {
        Objects.requireNonNull(modelo, "modelo");
        Objects.requireNonNull(versaoDoPrompt, "versaoDoPrompt");
        Objects.requireNonNull(latencia, "latencia");
        if (tokensDeEntrada < 0 || tokensDeSaida < 0) {
            throw new IllegalArgumentException("contagem de tokens não pode ser negativa");
        }
        custo = custo == null ? BigDecimal.ZERO : custo;
        moeda = moeda == null ? "USD" : moeda;
    }

    public int tokensTotais() {
        return tokensDeEntrada + tokensDeSaida;
    }
}
