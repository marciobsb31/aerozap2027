package br.com.aerozap.observability.logging;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Remove dados sensíveis de um texto de log.
 *
 * <p>Regra do projeto: log serve para diagnosticar, não para guardar dado de pessoa. Telefone do
 * WhatsApp, e-mail, CPF, cartão e credenciais são mascarados antes de qualquer gravação ou envio.
 * Os últimos quatro dígitos são preservados quando ajudam a reconhecer o registro sem identificar
 * a pessoa.
 *
 * <p>A classe não tem estado e é segura para uso concorrente.
 */
public final class MascaradorDeDadosSensiveis {

    private static final String OCULTO = "***";

    private record Regra(Pattern padrao, String substituicao) {
    }

    // A ordem importa: credenciais primeiro (são as mais específicas), depois identificadores.
    private static final List<Regra> REGRAS = List.of(
            // Authorization: Bearer <token>
            new Regra(Pattern.compile("(?i)\\b(bearer|basic)\\s+[A-Za-z0-9._~+/=-]{8,}"), "$1 " + OCULTO),
            // senha=..., token: ..., api-key=..., secret=...
            new Regra(Pattern.compile("(?i)\\b(senha|password|pwd|token|secret|api[-_]?key|chave)"
                    + "\\s*[=:]\\s*\"?[^\\s\",;}]{3,}\"?"), "$1=" + OCULTO),
            // Cartão em grupos de quatro: 4111 1111 1111 1111 (ou com - ou .)
            new Regra(Pattern.compile("(?<!\\d)\\d{4}([\\s.-])\\d{4}\\1\\d{4}\\1(\\d{4})(?!\\d)"),
                    "**** **** **** $2"),
            // E-mail: mantém o domínio, que raramente identifica alguém sozinho.
            new Regra(Pattern.compile("(?i)\\b[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b"), OCULTO + "@$1"),
            // CPF formatado: 000.000.000-00
            new Regra(Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b"), "***.***.***-**"),
            // CNPJ formatado: 00.000.000/0000-00
            new Regra(Pattern.compile("\\b\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}\\b"), "**.***.***/****-**"),
            // Telefone com DDI/DDD, com ou sem pontuação: +55 (11) 99999-8888
            new Regra(Pattern.compile("(?<!\\d)(?:\\+\\d{1,3}[\\s.-]?)?\\(?\\d{2}\\)?[\\s.-]?\\d{4,5}[\\s.-]?(\\d{4})(?!\\d)"),
                    OCULTO + "$1"),
            // Sequências longas de dígitos (cartão, documento): preserva só os quatro últimos.
            // A guarda contra letras e hífen evita estragar UUID (identificador de correlação).
            new Regra(Pattern.compile("(?<![\\dA-Za-z-])\\d{8,}(\\d{4})(?![\\dA-Za-z-])"), OCULTO + "$1"));

    private MascaradorDeDadosSensiveis() {
    }

    /**
     * Devolve o texto sem dados sensíveis. Entrada nula devolve nulo, para poder ser usada
     * diretamente onde o valor é opcional.
     */
    public static String mascarar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        String resultado = texto;
        for (Regra regra : REGRAS) {
            resultado = regra.padrao().matcher(resultado).replaceAll(regra.substituicao());
        }
        return resultado;
    }

    /** Mesma regra aplicada a um valor qualquer; só textos são alterados. */
    public static Object mascararValor(Object valor) {
        return valor instanceof String texto ? mascarar(texto) : valor;
    }
}
