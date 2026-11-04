package br.com.aerozap.conversationai.llm;

/**
 * O provedor de IA não respondeu. Não é erro do usuário e nunca consome franquia: a resposta é
 * pedir o pedido por texto ou tentar de novo (RF-045).
 */
public class LlmIndisponivelException extends RuntimeException {

    public LlmIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public LlmIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
