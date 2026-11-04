package br.com.aerozap.conversationai.interpretacao.extracao;

/**
 * Uma menção a lugar encontrada na frase.
 *
 * @param iata código resolvido
 * @param inicio posição no texto normalizado, para decidir quem é origem e quem é destino
 * @param papel o que a preposição antes dele indica
 */
public record Lugar(String iata, int inicio, Papel papel) {

    /** O que a frase diz sobre o lugar. */
    public enum Papel {
        ORIGEM,
        DESTINO,
        INDEFINIDO
    }
}
