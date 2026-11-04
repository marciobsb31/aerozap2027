package br.com.aerozap.conversationai.interpretacao;

/** Por que o interpretador não vai adiante. */
public enum MotivoDaRecusa {
    /** A mensagem tenta mudar as regras, o plano ou ver dados de outra pessoa (RF-046). */
    TENTATIVA_DE_INJECAO,
    /** Não é um pedido de passagem. */
    FORA_DE_ESCOPO,
    /** Não deu para entender o suficiente nem para formular uma pergunta útil. */
    NAO_COMPREENDIDO
}
