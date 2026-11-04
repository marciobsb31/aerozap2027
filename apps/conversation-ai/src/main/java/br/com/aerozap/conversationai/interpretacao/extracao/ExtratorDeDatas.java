package br.com.aerozap.conversationai.interpretacao.extracao;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encontra datas em português e espanhol, inclusive as relativas.
 *
 * <p>Duas decisões que mudam o comportamento do produto:
 *
 * <p><strong>Ano omitido vira a próxima ocorrência.</strong> "10/07" em dezembro é julho do ano
 * que vem, não julho que já passou. Ninguém compra passagem para o passado.
 *
 * <p><strong>Mês sem dia não é data.</strong> "em julho" fica registrado como mês sem dia, e isso
 * leva a uma pergunta — nunca a uma pesquisa com um dia inventado (RF-026, RF-041). Chutar o dia 1
 * seria o tipo de ajuda que gasta a consulta de alguém com uma resposta errada.
 */
public final class ExtratorDeDatas {

    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("janeiro", 1), Map.entry("enero", 1),
            Map.entry("fevereiro", 2), Map.entry("febrero", 2),
            Map.entry("marco", 3), Map.entry("marzo", 3),
            Map.entry("abril", 4),
            Map.entry("maio", 5), Map.entry("mayo", 5),
            Map.entry("junho", 6), Map.entry("junio", 6),
            Map.entry("julho", 7), Map.entry("julio", 7),
            Map.entry("agosto", 8),
            Map.entry("setembro", 9), Map.entry("septiembre", 9),
            Map.entry("outubro", 10), Map.entry("octubre", 10),
            Map.entry("novembro", 11), Map.entry("noviembre", 11),
            Map.entry("dezembro", 12), Map.entry("diciembre", 12));

    private static final Map<String, DayOfWeek> DIAS_DA_SEMANA = Map.ofEntries(
            Map.entry("segunda", DayOfWeek.MONDAY), Map.entry("lunes", DayOfWeek.MONDAY),
            Map.entry("terca", DayOfWeek.TUESDAY), Map.entry("martes", DayOfWeek.TUESDAY),
            Map.entry("quarta", DayOfWeek.WEDNESDAY), Map.entry("miercoles", DayOfWeek.WEDNESDAY),
            Map.entry("quinta", DayOfWeek.THURSDAY), Map.entry("jueves", DayOfWeek.THURSDAY),
            Map.entry("sexta", DayOfWeek.FRIDAY), Map.entry("viernes", DayOfWeek.FRIDAY),
            Map.entry("sabado", DayOfWeek.SATURDAY),
            Map.entry("domingo", DayOfWeek.SUNDAY));

    private static final Pattern ISO = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b");
    private static final Pattern NUMERICA = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b");
    private static final Pattern DIA_E_MES = Pattern.compile(
            "\\b(\\d{1,2})\\s*(?:de\\s+)?(" + String.join("|", MESES.keySet()) + ")\\b"
                    + "(?:\\s+(?:de\\s+)?(\\d{4}))?");
    private static final Pattern MES_SOZINHO = Pattern.compile(
            "\\b(?:em|no mes de|en|el mes de)\\s+(" + String.join("|", MESES.keySet()) + ")\\b");
    private static final Pattern DIA_DA_SEMANA = Pattern.compile(
            "\\b(?:proxima|proximo|que vem|el proximo|la proxima)?\\s*("
                    + String.join("|", DIAS_DA_SEMANA.keySet()) + ")\\b");
    /** "volto dia 20", "regreso el 20": dia solto, que só faz sentido junto de uma data anterior. */
    private static final Pattern DIA_SOLTO = Pattern.compile("\\b(?:dia|el dia|el)\\s+(\\d{1,2})\\b");
    private static final Pattern PEDIDO_DE_VOLTA = Pattern.compile(
            "\\b(?:ida e volta|ida y vuelta|volto|voltando|de volta|retorno|regreso|vuelvo|vuelta)\\b");

    private ExtratorDeDatas() {
    }

    public static DatasEncontradas extrair(String texto, LocalDate hoje) {
        List<Achado> achados = new ArrayList<>();
        boolean[] ocupado = new boolean[texto.length()];

        achar(ISO, texto, ocupado, achados, m -> LocalDate.of(
                Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));

        achar(NUMERICA, texto, ocupado, achados, m -> {
            int dia = Integer.parseInt(m.group(1));
            int mes = Integer.parseInt(m.group(2));
            Integer ano = m.group(3) == null ? null : normalizarAno(Integer.parseInt(m.group(3)));
            return montar(dia, mes, ano, hoje);
        });

        achar(DIA_E_MES, texto, ocupado, achados, m -> {
            int dia = Integer.parseInt(m.group(1));
            int mes = MESES.get(m.group(2));
            Integer ano = m.group(3) == null ? null : Integer.parseInt(m.group(3));
            return montar(dia, mes, ano, hoje);
        });

        achar(DIA_DA_SEMANA, texto, ocupado, achados, m -> proximoDiaDaSemana(hoje, DIAS_DA_SEMANA.get(m.group(1))));

        acharRelativasFixas(texto, hoje, ocupado, achados);
        acharDiaSolto(texto, ocupado, achados);

        boolean mesSemDia = false;
        Matcher mes = MES_SOZINHO.matcher(texto);
        while (mes.find()) {
            if (livre(ocupado, mes.start(1), mes.end(1))) {
                mesSemDia = true;
            }
        }

        achados.sort((a, b) -> Integer.compare(a.posicao(), b.posicao()));
        List<LocalDate> datas = achados.stream().map(Achado::data).distinct().toList();
        boolean pediuVolta = PEDIDO_DE_VOLTA.matcher(texto).find() || datas.size() > 1;
        return new DatasEncontradas(datas, mesSemDia, pediuVolta);
    }

    /**
     * Resolve "dia 20" usando a data já encontrada antes dele: é assim que se diz a volta em
     * português e em espanhol. Sem uma data anterior, um dia solto não vira nada — é ambíguo
     * demais para chutar.
     */
    private static void acharDiaSolto(String texto, boolean[] ocupado, List<Achado> achados) {
        if (achados.isEmpty()) {
            return;
        }
        achados.sort((a, b) -> Integer.compare(a.posicao(), b.posicao()));
        Matcher m = DIA_SOLTO.matcher(texto);
        while (m.find()) {
            if (!livre(ocupado, m.start(), m.end())) {
                continue;
            }
            LocalDate base = null;
            for (Achado anterior : achados) {
                if (anterior.posicao() < m.start()) {
                    base = anterior.data();
                }
            }
            if (base == null) {
                continue;
            }
            int dia = Integer.parseInt(m.group(1));
            try {
                LocalDate candidata = base.withDayOfMonth(dia);
                if (candidata.isBefore(base)) {
                    candidata = base.plusMonths(1).withDayOfMonth(dia);
                }
                marcar(ocupado, m.start(), m.end());
                achados.add(new Achado(candidata, m.start()));
            } catch (RuntimeException e) {
                // dia que não existe naquele mês: ignora
            }
        }
    }

    private static void acharRelativasFixas(String texto, LocalDate hoje, boolean[] ocupado,
                                            List<Achado> achados) {
        registrarSePresente(texto, "depois de amanha", hoje.plusDays(2), ocupado, achados);
        registrarSePresente(texto, "pasado manana", hoje.plusDays(2), ocupado, achados);
        registrarSePresente(texto, "amanha", hoje.plusDays(1), ocupado, achados);
        registrarSePresente(texto, "manana", hoje.plusDays(1), ocupado, achados);
        registrarSePresente(texto, "hoje", hoje, ocupado, achados);
        registrarSePresente(texto, "hoy", hoje, ocupado, achados);
        registrarSePresente(texto, "semana que vem", hoje.plusWeeks(1), ocupado, achados);
        registrarSePresente(texto, "proxima semana", hoje.plusWeeks(1), ocupado, achados);
    }

    private static void registrarSePresente(String texto, String expressao, LocalDate data,
                                            boolean[] ocupado, List<Achado> achados) {
        int pos = texto.indexOf(expressao);
        while (pos >= 0) {
            if (livre(ocupado, pos, pos + expressao.length())) {
                marcar(ocupado, pos, pos + expressao.length());
                achados.add(new Achado(data, pos));
            }
            pos = texto.indexOf(expressao, pos + 1);
        }
    }

    private static void achar(Pattern padrao, String texto, boolean[] ocupado, List<Achado> achados,
                              java.util.function.Function<Matcher, LocalDate> conversao) {
        Matcher m = padrao.matcher(texto);
        while (m.find()) {
            if (!livre(ocupado, m.start(), m.end())) {
                continue;
            }
            LocalDate data;
            try {
                data = conversao.apply(m);
            } catch (RuntimeException e) {
                continue; // 31/02 e parecidos: não é data, segue adiante
            }
            if (data != null) {
                marcar(ocupado, m.start(), m.end());
                achados.add(new Achado(data, m.start()));
            }
        }
    }

    private static LocalDate montar(int dia, int mes, Integer ano, LocalDate hoje) {
        if (ano != null) {
            return LocalDate.of(ano, mes, dia);
        }
        LocalDate candidata = LocalDate.of(hoje.getYear(), mes, dia);
        return candidata.isBefore(hoje) ? candidata.plusYears(1) : candidata;
    }

    private static int normalizarAno(int ano) {
        return ano < 100 ? 2000 + ano : ano;
    }

    private static LocalDate proximoDiaDaSemana(LocalDate hoje, DayOfWeek alvo) {
        LocalDate data = hoje.plusDays(1);
        while (data.getDayOfWeek() != alvo) {
            data = data.plusDays(1);
        }
        return data;
    }

    private static boolean livre(boolean[] ocupado, int inicio, int fim) {
        for (int i = inicio; i < Math.min(fim, ocupado.length); i++) {
            if (ocupado[i]) {
                return false;
            }
        }
        return true;
    }

    private static void marcar(boolean[] ocupado, int inicio, int fim) {
        for (int i = inicio; i < Math.min(fim, ocupado.length); i++) {
            ocupado[i] = true;
        }
    }

    private record Achado(LocalDate data, int posicao) {
    }
}
