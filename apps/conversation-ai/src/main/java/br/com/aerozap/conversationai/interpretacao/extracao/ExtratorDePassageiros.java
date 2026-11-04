package br.com.aerozap.conversationai.interpretacao.extracao;

import br.com.aerozap.contracts.search.CabinClass;
import br.com.aerozap.contracts.search.Passengers;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quantas pessoas viajam e em que classe.
 *
 * <p>O padrão é um adulto. Não é chute: é o caso esmagadoramente mais comum, e errar para mais
 * seria pior — pesquisa de duas pessoas devolve preço de duas, e a pessoa só descobre o engano
 * ao ver o valor.
 */
public final class ExtratorDePassageiros {

    private static final Map<String, Integer> NUMEROS_POR_EXTENSO = Map.ofEntries(
            Map.entry("um", 1), Map.entry("uma", 1), Map.entry("uno", 1), Map.entry("una", 1),
            Map.entry("dois", 2), Map.entry("duas", 2), Map.entry("dos", 2),
            Map.entry("tres", 3),
            Map.entry("quatro", 4), Map.entry("cuatro", 4),
            Map.entry("cinco", 5),
            Map.entry("seis", 6),
            Map.entry("sete", 7), Map.entry("siete", 7),
            Map.entry("oito", 8), Map.entry("ocho", 8),
            Map.entry("nove", 9), Map.entry("nueve", 9));

    private static final String QUANTIDADE = "(\\d+|" + String.join("|", NUMEROS_POR_EXTENSO.keySet()) + ")";

    private static final Pattern ADULTOS = Pattern.compile(QUANTIDADE + "\\s+adult[oa]s?\\b");
    private static final Pattern CRIANCAS = Pattern.compile(QUANTIDADE + "\\s+(?:crianc[ao]s?|nin[oa]s?)\\b");
    private static final Pattern BEBES = Pattern.compile(QUANTIDADE + "\\s+(?:bebes?|bebe|infantes?)\\b");
    private static final Pattern SOMOS = Pattern.compile("\\b(?:somos|seremos)\\s+" + QUANTIDADE + "\\b");
    private static final Pattern PESSOAS = Pattern.compile(QUANTIDADE + "\\s+pessoas?\\b");
    private static final Pattern SOZINHO = Pattern.compile("\\b(?:para mim|pra mim|para mi|sozinho|solo)\\b");

    private ExtratorDePassageiros() {
    }

    public static Passengers extrair(String texto) {
        int adultos = quantidade(ADULTOS, texto).orElse(0);
        int criancas = quantidade(CRIANCAS, texto).orElse(0);
        int bebes = quantidade(BEBES, texto).orElse(0);

        if (adultos == 0) {
            adultos = quantidade(SOMOS, texto)
                    .or(() -> quantidade(PESSOAS, texto))
                    .map(total -> Math.max(1, total - criancas - bebes))
                    .orElse(0);
        }
        if (adultos == 0 && SOZINHO.matcher(texto).find()) {
            adultos = 1;
        }
        if (adultos == 0) {
            adultos = 1;
        }

        int total = adultos + criancas + bebes;
        if (total > Passengers.MAXIMO_POR_PESQUISA || bebes > adultos) {
            // Frase que não fecha: devolve o padrão e deixa a validação do contrato decidir adiante.
            return Passengers.umAdulto();
        }
        return new Passengers(adultos, criancas, bebes);
    }

    /** Classe de cabine, quando a pessoa disser. */
    public static Optional<CabinClass> cabine(String texto) {
        if (texto.contains("primeira classe") || texto.contains("primera clase")) {
            return Optional.of(CabinClass.FIRST);
        }
        if (texto.contains("premium")) {
            return Optional.of(CabinClass.PREMIUM_ECONOMY);
        }
        if (texto.contains("executiva") || texto.contains("ejecutiva") || texto.contains("business")) {
            return Optional.of(CabinClass.BUSINESS);
        }
        if (texto.contains("economica") || texto.contains("economy")) {
            return Optional.of(CabinClass.ECONOMY);
        }
        return Optional.empty();
    }

    private static Optional<Integer> quantidade(Pattern padrao, String texto) {
        Matcher m = padrao.matcher(texto);
        if (!m.find()) {
            return Optional.empty();
        }
        String valor = m.group(1);
        if (valor.matches("\\d+")) {
            try {
                return Optional.of(Integer.parseInt(valor));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(NUMEROS_POR_EXTENSO.get(valor));
    }
}
