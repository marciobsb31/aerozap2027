package br.com.aerozap.conversationai.interpretacao;

/**
 * Textos que o interpretador devolve, nos dois idiomas.
 *
 * <p>Ficam juntos e curtos de propósito: é o que a pessoa lê no WhatsApp. Uma pergunta por vez,
 * sem jargão, sem pedir o que ela já disse (RF-041).
 */
final class Perguntas {

    private Perguntas() {
    }

    static String para(CampoFaltante campo, String idioma) {
        boolean espanhol = "es".equals(idioma);
        return switch (campo) {
            case DESTINO -> espanhol ? "¿Para dónde querés viajar?" : "Para onde você quer viajar?";
            case ORIGEM -> espanhol ? "¿Desde qué ciudad salís?" : "De qual cidade você sai?";
            case DATA_DE_IDA -> espanhol ? "¿Qué día querés viajar?" : "Em que dia você quer viajar?";
            case DATA_DE_VOLTA -> espanhol ? "¿Qué día querés volver?" : "Em que dia você quer voltar?";
        };
    }

    static String recusaDeInjecao(String idioma) {
        return "es".equals(idioma)
                ? "Solo puedo ayudarte a buscar vuelos. Contame origen, destino y fecha."
                : "Só consigo ajudar com busca de voos. Me diga origem, destino e data.";
    }

    static String naoEntendi(String idioma) {
        return "es".equals(idioma)
                ? "No entendí el pedido. Probá así: \"de Brasilia a Lisboa el 10/07, 1 adulto\"."
                : "Não entendi o pedido. Tente assim: \"de Brasília para Lisboa em 10/07, 1 adulto\".";
    }
}
