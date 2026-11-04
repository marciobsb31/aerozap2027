package br.com.aerozap.conversationai.llm;

/**
 * Porta de saída para um provedor de modelo de linguagem.
 *
 * <p>Mesma ideia da porta de provedores de voo: trocar o fornecedor não pode mexer em quem usa.
 * A diferença é que aqui a porta também limita o que o modelo <em>pode</em> fazer — ela devolve
 * texto, e só.
 */
public interface ProvedorDeLlm {

    /** Identificação do modelo, para registro e comparação de custo. */
    String modelo();

    /**
     * Chama o modelo.
     *
     * @throws LlmIndisponivelException quando o provedor falha, excede o tempo ou recusa; quem
     *     chama cai para o caminho sem IA e responde pedindo texto (RF-045)
     */
    RespostaDoLlm completar(PedidoAoLlm pedido);
}
