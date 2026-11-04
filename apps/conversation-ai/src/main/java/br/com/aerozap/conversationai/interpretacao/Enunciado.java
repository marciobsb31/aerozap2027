package br.com.aerozap.conversationai.interpretacao;

import java.util.Objects;

/**
 * O que a pessoa escreveu ou falou, como chegou.
 *
 * @param texto conteúdo da mensagem, ou a transcrição do áudio
 * @param idioma {@code pt-BR} ou {@code es}
 */
public record Enunciado(String texto, String idioma) {

    /** Mensagem maior que isto não é pedido de passagem; é texto colado, e texto colado é risco. */
    public static final int LIMITE_DE_CARACTERES = 2000;

    public Enunciado {
        Objects.requireNonNull(texto, "texto");
        Objects.requireNonNull(idioma, "idioma");
        if (texto.isBlank()) {
            throw new IllegalArgumentException("enunciado vazio");
        }
        if (texto.length() > LIMITE_DE_CARACTERES) {
            throw new IllegalArgumentException("enunciado acima de " + LIMITE_DE_CARACTERES + " caracteres");
        }
        if (!idioma.equals("pt-BR") && !idioma.equals("es")) {
            throw new IllegalArgumentException("idioma precisa ser pt-BR ou es: " + idioma);
        }
    }

    public static Enunciado pt(String texto) {
        return new Enunciado(texto, "pt-BR");
    }

    public static Enunciado es(String texto) {
        return new Enunciado(texto, "es");
    }
}
