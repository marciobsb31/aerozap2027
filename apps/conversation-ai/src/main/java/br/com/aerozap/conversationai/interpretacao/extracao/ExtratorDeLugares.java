package br.com.aerozap.conversationai.interpretacao.extracao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descobre origem e destino na frase.
 *
 * <p>A pista principal é a preposição: "de Brasília" é origem, "para Lisboa" é destino. Quando não
 * há pista e há dois lugares, vale a ordem — "Brasília Lisboa" é quem diria o bilhete. Com um
 * lugar só e sem pista, é destino: ninguém escreve "quero sair de Brasília" querendo dizer para
 * onde vai, mas muita gente escreve "quero ir para Lisboa".
 */
public final class ExtratorDeLugares {

    /**
     * O \\b antes da preposição não é detalhe: sem ele, "curitiba " termina em "a " e a cidade
     * seguinte vira destino por engano. O artigo opcional no fim cobre "para o Rio", "a la".
     */
    private static final String ARTIGO = "(?:\\s+(?:o|a|os|as|el|la|los|las))?\\s*$";
    private static final Pattern PISTA_DE_ORIGEM = Pattern.compile(
            "\\b(?:saindo de|partindo de|saliendo de|desde|de|from)" + ARTIGO);
    private static final Pattern PISTA_DE_DESTINO = Pattern.compile(
            "\\b(?:com destino a|rumo a|hasta|para|pra|ate|hacia|a|to)" + ARTIGO);

    private ExtratorDeLugares() {
    }

    /** Todas as menções a lugar, na ordem em que aparecem, sem repetir o mesmo código. */
    public static List<Lugar> extrair(String textoNormalizado) {
        List<Lugar> achados = new ArrayList<>();
        boolean[] ocupado = new boolean[textoNormalizado.length()];

        for (String nome : Aeroportos.nomesConhecidos()) {
            Matcher m = Pattern.compile("\\b" + Pattern.quote(nome) + "\\b").matcher(textoNormalizado);
            while (m.find()) {
                if (trechoLivre(ocupado, m.start(), m.end())) {
                    marcar(ocupado, m.start(), m.end());
                    String iata = Aeroportos.iataDe(nome).orElseThrow();
                    achados.add(new Lugar(iata, m.start(), papelPeloQueVemAntes(textoNormalizado, m.start())));
                }
            }
        }
        achados.sort((a, b) -> Integer.compare(a.inicio(), b.inicio()));
        return semRepetidos(achados);
    }

    /** Origem, se a frase deixar claro. */
    public static Optional<String> origem(List<Lugar> lugares) {
        Optional<String> marcada = porPapel(lugares, Lugar.Papel.ORIGEM);
        if (marcada.isPresent()) {
            return marcada;
        }
        // Dois lugares sem pista nenhuma: o primeiro é de onde se sai.
        if (lugares.size() == 2 && lugares.stream().allMatch(l -> l.papel() == Lugar.Papel.INDEFINIDO)) {
            return Optional.of(lugares.get(0).iata());
        }
        // "GRU para LIS": um está marcado como destino, sobra o outro. Só vale com dois lugares.
        return oComplementar(lugares, Lugar.Papel.DESTINO);
    }

    /** Destino, se a frase deixar claro. */
    public static Optional<String> destino(List<Lugar> lugares) {
        Optional<String> marcado = porPapel(lugares, Lugar.Papel.DESTINO);
        if (marcado.isPresent()) {
            return marcado;
        }
        if (lugares.size() == 2 && lugares.stream().allMatch(l -> l.papel() == Lugar.Papel.INDEFINIDO)) {
            return Optional.of(lugares.get(1).iata());
        }
        if (lugares.size() == 1 && lugares.getFirst().papel() == Lugar.Papel.INDEFINIDO) {
            return Optional.of(lugares.getFirst().iata());
        }
        // "de Brasília para o Rio": a origem está marcada, sobra o outro.
        return oComplementar(lugares, Lugar.Papel.ORIGEM);
    }

    /**
     * Com exatamente dois lugares e um papel já conhecido, o outro é o papel que falta. É a
     * leitura natural: ninguém cita duas cidades num pedido de passagem sem que uma seja de onde
     * sai e a outra para onde vai.
     */
    private static Optional<String> oComplementar(List<Lugar> lugares, Lugar.Papel papelConhecido) {
        if (lugares.size() != 2) {
            return Optional.empty();
        }
        long conhecidos = lugares.stream().filter(l -> l.papel() == papelConhecido).count();
        if (conhecidos != 1) {
            return Optional.empty();
        }
        return lugares.stream()
                .filter(l -> l.papel() == Lugar.Papel.INDEFINIDO)
                .map(Lugar::iata)
                .findFirst();
    }

    private static Optional<String> porPapel(List<Lugar> lugares, Lugar.Papel papel) {
        return lugares.stream().filter(l -> l.papel() == papel).map(Lugar::iata).findFirst();
    }

    private static Lugar.Papel papelPeloQueVemAntes(String texto, int inicio) {
        String antes = texto.substring(Math.max(0, inicio - 20), inicio);
        if (PISTA_DE_ORIGEM.matcher(antes).find()) {
            return Lugar.Papel.ORIGEM;
        }
        if (PISTA_DE_DESTINO.matcher(antes).find()) {
            return Lugar.Papel.DESTINO;
        }
        return Lugar.Papel.INDEFINIDO;
    }

    private static List<Lugar> semRepetidos(List<Lugar> lugares) {
        List<Lugar> saida = new ArrayList<>();
        for (Lugar lugar : lugares) {
            if (saida.stream().noneMatch(j -> j.iata().equals(lugar.iata()))) {
                saida.add(lugar);
            }
        }
        return saida;
    }

    private static boolean trechoLivre(boolean[] ocupado, int inicio, int fim) {
        for (int i = inicio; i < fim; i++) {
            if (ocupado[i]) {
                return false;
            }
        }
        return true;
    }

    private static void marcar(boolean[] ocupado, int inicio, int fim) {
        for (int i = inicio; i < fim; i++) {
            ocupado[i] = true;
        }
    }
}
