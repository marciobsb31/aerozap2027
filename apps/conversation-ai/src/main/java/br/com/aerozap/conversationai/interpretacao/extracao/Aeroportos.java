package br.com.aerozap.conversationai.interpretacao.extracao;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Dicionário de cidades e aeroportos conhecidos, em português e espanhol, para código IATA.
 *
 * <p>Não é uma base de aeroportos: é o vocabulário das rotas que a plataforma espera atender no
 * começo. A base completa vem com o provedor; aqui o objetivo é entender a frase. Cidade com mais
 * de um aeroporto resolve para o internacional principal — "São Paulo" vira GRU, e quem quiser
 * Congonhas diz Congonhas.
 */
public final class Aeroportos {

    private static final Map<String, String> POR_NOME = new LinkedHashMap<>();

    private static void registrar(String iata, String... nomes) {
        for (String nome : nomes) {
            POR_NOME.put(normalizar(nome), iata);
        }
    }

    static {
        // Brasil
        registrar("BSB", "brasilia", "bsb");
        registrar("GRU", "sao paulo", "guarulhos", "gru", "cumbica");
        registrar("CGH", "congonhas", "cgh");
        registrar("VCP", "viracopos", "campinas", "vcp");
        registrar("GIG", "rio de janeiro", "rio", "galeao", "gig");
        registrar("SDU", "santos dumont", "sdu");
        registrar("CNF", "belo horizonte", "confins", "bh", "cnf");
        registrar("SSA", "salvador", "ssa");
        registrar("REC", "recife", "rec");
        registrar("FOR", "fortaleza", "for");
        registrar("POA", "porto alegre", "poa");
        registrar("CWB", "curitiba", "cwb");
        registrar("FLN", "florianopolis", "floripa", "fln");
        registrar("MAO", "manaus", "mao");
        registrar("BEL", "belem", "bel");
        registrar("NAT", "natal", "nat");
        registrar("MCZ", "maceio", "mcz");
        registrar("VIX", "vitoria", "vix");
        registrar("GYN", "goiania", "gyn");
        registrar("CGB", "cuiaba", "cgb");
        registrar("CGR", "campo grande", "cgr");
        registrar("SLZ", "sao luis", "slz");
        registrar("JPA", "joao pessoa", "jpa");
        registrar("AJU", "aracaju", "aju");
        registrar("THE", "teresina", "the");
        registrar("PMW", "palmas", "pmw");
        registrar("PVH", "porto velho", "pvh");
        registrar("RBR", "rio branco", "rbr");
        registrar("BVB", "boa vista", "bvb");
        registrar("MCP", "macapa", "mcp");
        registrar("IGU", "foz do iguacu", "foz", "igu");
        registrar("NVT", "navegantes", "balneario camboriu", "nvt");
        registrar("IOS", "ilheus", "ios");
        registrar("BPS", "porto seguro", "bps");
        registrar("UDI", "uberlandia", "udi");
        registrar("RAO", "ribeirao preto", "rao");
        registrar("LDB", "londrina", "ldb");

        // Europa
        registrar("LIS", "lisboa", "lisbon", "lis");
        registrar("OPO", "porto portugal", "oporto", "opo");
        registrar("MAD", "madri", "madrid", "mad");
        registrar("BCN", "barcelona", "bcn");
        registrar("CDG", "paris", "cdg");
        registrar("LHR", "londres", "london", "lhr");
        registrar("FCO", "roma", "rome", "fco");
        registrar("MXP", "milao", "milan", "mxp");
        registrar("FRA", "frankfurt", "fra");
        registrar("AMS", "amsterda", "amsterdam", "ams");
        registrar("ZRH", "zurique", "zurich", "zrh");

        // Américas e outros
        registrar("MIA", "miami", "mia");
        registrar("JFK", "nova york", "nueva york", "new york", "jfk");
        registrar("MCO", "orlando", "mco");
        registrar("EZE", "buenos aires", "eze");
        registrar("SCL", "santiago", "scl");
        registrar("MVD", "montevideu", "montevideo", "mvd");
        registrar("ASU", "assuncao", "asuncion", "asu");
        registrar("BOG", "bogota", "bog");
        registrar("LIM", "lima", "lim");
        registrar("MEX", "cidade do mexico", "ciudad de mexico", "mex");
        registrar("CUN", "cancun", "cun");
        registrar("YYZ", "toronto", "yyz");
        registrar("DXB", "dubai", "dxb");
        registrar("JNB", "joanesburgo", "johannesburgo", "jnb");
    }

    private Aeroportos() {
    }

    /** Minúsculas e sem acento: "Brasília" e "brasilia" são a mesma coisa para quem escreve correndo. */
    public static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }

    public static Optional<String> iataDe(String nomeNormalizado) {
        return Optional.ofNullable(POR_NOME.get(nomeNormalizado));
    }

    /** Nomes conhecidos, do mais longo para o mais curto: "porto alegre" antes de "porto". */
    public static Iterable<String> nomesConhecidos() {
        return POR_NOME.keySet().stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();
    }
}
