package br.com.aerozap.conversationai.llm;

import java.util.Objects;

/**
 * Resposta do modelo: texto e o que custou.
 *
 * <p>Só texto. A porta não tem como o modelo chamar ferramenta, consultar banco ou disparar ação —
 * e é assim que a allowlist de ferramentas começa vazia e explícita (RF-046). Quando uma
 * ferramenta for necessária, ela entra como tipo novo e decisão registrada, não por acidente.
 *
 * @param conteudo texto devolvido pelo modelo
 * @param uso tokens, latência e custo
 */
public record RespostaDoLlm(String conteudo, UsoDeLlm uso) {

    public RespostaDoLlm {
        Objects.requireNonNull(conteudo, "conteudo");
        Objects.requireNonNull(uso, "uso");
    }
}
