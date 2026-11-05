package br.com.aerozap.conversationai.interpretacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.contracts.search.CabinClass;
import br.com.aerozap.contracts.search.SearchIntent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Roda o conjunto dourado contra o interpretador e registra o acerto.
 *
 * <p>O conjunto tem duas metades, e a segunda é a mais útil: os casos marcados
 * {@code alcanceV0: false} são os que o motor de regras <strong>não</strong> deve acertar —
 * refinamento com contexto, destino por região, data por evento cultural, cidade fora do
 * dicionário. Eles não quebram o build; eles medem a lacuna que o modelo de linguagem precisa
 * fechar no M4. Sem essa linha de base, "a IA acerta 85%" é um número sem régua.
 *
 * <p>Os casos {@code alcanceV0: true} são contrato: se um deixar de passar, o build quebra.
 */
@DisplayName("Conjunto dourado da interpretação")
class ConjuntoDouradoTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Interpretador INTERPRETADOR = new InterpretadorDeterministico();
    private static final List<Resultado> RESULTADOS = new ArrayList<>();

    private record Resultado(String id, String idioma, String categoria, boolean noAlcance,
                             boolean acertou, String observado) {
    }

    private static Path raizDoProjeto() {
        Path atual = Path.of("").toAbsolutePath();
        while (atual != null) {
            if (Files.isDirectory(atual.resolve("tests/conjunto-dourado"))) {
                return atual;
            }
            atual = atual.getParent();
        }
        throw new IllegalStateException("não encontrei tests/conjunto-dourado");
    }

    @Test
    @DisplayName("todo caso no alcance do v0 é acertado")
    void todoCasoNoAlcanceEAcertado() throws IOException {
        JsonNode conjunto = JSON.readTree(
                Files.readString(raizDoProjeto().resolve("tests/conjunto-dourado/interpretacao-v0.json")));

        List<String> falhas = new ArrayList<>();
        for (JsonNode caso : conjunto.get("casos")) {
            String id = caso.get("id").asString();
            boolean noAlcance = caso.get("alcanceV0").asBoolean();
            Enunciado enunciado = new Enunciado(caso.get("enunciado").asString(),
                    caso.get("idioma").asString());
            ContextoDaInterpretacao contexto =
                    ContextoDaInterpretacao.em(LocalDate.parse(caso.get("hoje").asString()));
            if (caso.has("contextoAnterior")) {
                contexto = contexto.continuando(lerParcial(caso.get("contextoAnterior")));
            }

            ResultadoDaInterpretacao obtido = INTERPRETADOR.interpretar(enunciado, contexto);
            Optional<String> erro = comparar(caso.get("esperado"), obtido);

            RESULTADOS.add(new Resultado(id, caso.get("idioma").asString(),
                    caso.get("categoria").asString(), noAlcance, erro.isEmpty(), descrever(obtido)));

            if (noAlcance && erro.isPresent()) {
                falhas.add(id + ": " + erro.get());
            }
        }

        assertThat(falhas).as("casos no alcance do v0 que falharam").isEmpty();
    }

    @Test
    @DisplayName("o conjunto cobre as cinco categorias, nos dois idiomas")
    void conjuntoCobreAsCategorias() throws IOException {
        JsonNode conjunto = JSON.readTree(
                Files.readString(raizDoProjeto().resolve("tests/conjunto-dourado/interpretacao-v0.json")));

        List<String> categorias = new ArrayList<>();
        List<String> idiomas = new ArrayList<>();
        for (JsonNode caso : conjunto.get("casos")) {
            categorias.add(caso.get("categoria").asString());
            idiomas.add(caso.get("idioma").asString());
        }

        assertThat(categorias).contains("sucesso", "ambiguidade", "refinamento", "datas-relativas", "injecao");
        assertThat(idiomas).contains("pt-BR", "es");
    }

    /** Escreve a medição onde o relatório de entrega possa citá-la. */
    @AfterAll
    static void registrarAcuracia() throws IOException {
        if (RESULTADOS.isEmpty()) {
            return;
        }
        StringBuilder relatorio = new StringBuilder();
        relatorio.append("# Acerto no conjunto dourado — ").append(INTERPRETADOR.nome()).append("\n\n");
        relatorio.append(linha("Recorte", "Casos", "Acertos", "Acerto"));
        relatorio.append(linha("---", "---:", "---:", "---:"));

        relatorio.append(resumo("Total", RESULTADOS));
        relatorio.append(resumo("No alcance do v0", RESULTADOS.stream().filter(Resultado::noAlcance).toList()));
        relatorio.append(resumo("Além do v0 (lacuna para a IA)",
                RESULTADOS.stream().filter(r -> !r.noAlcance()).toList()));

        Map<String, List<Resultado>> porCategoria = new LinkedHashMap<>();
        Map<String, List<Resultado>> porIdioma = new LinkedHashMap<>();
        for (Resultado r : RESULTADOS) {
            porCategoria.computeIfAbsent(r.categoria(), k -> new ArrayList<>()).add(r);
            porIdioma.computeIfAbsent(r.idioma(), k -> new ArrayList<>()).add(r);
        }
        porCategoria.forEach((nome, lista) -> relatorio.append(resumo("categoria: " + nome, lista)));
        porIdioma.forEach((nome, lista) -> relatorio.append(resumo("idioma: " + nome, lista)));

        relatorio.append("\n## Casos além do alcance do v0\n\n");
        RESULTADOS.stream().filter(r -> !r.noAlcance()).forEach(r ->
                relatorio.append("- `").append(r.id()).append("` → ").append(r.observado())
                        .append(r.acertou() ? " (acertou mesmo assim)" : "").append("\n"));

        Path saida = Path.of("target", "acuracia-conjunto-dourado.md");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, relatorio.toString());
        System.out.println(relatorio);
    }

    private static String resumo(String rotulo, List<Resultado> casos) {
        long acertos = casos.stream().filter(Resultado::acertou).count();
        String percentual = casos.isEmpty() ? "—"
                : String.format("%.0f%%", 100.0 * acertos / casos.size());
        return linha(rotulo, String.valueOf(casos.size()), String.valueOf(acertos), percentual);
    }

    private static String linha(String... colunas) {
        return "| " + String.join(" | ", colunas) + " |\n";
    }

    /** Intenção já entendida em mensagens anteriores da conversa. */
    private static IntencaoParcial lerParcial(JsonNode no) {
        IntencaoParcial parcial = IntencaoParcial.vazia();
        if (no.has("origem")) {
            parcial = parcial.comOrigem(no.get("origem").asString());
        }
        if (no.has("destino")) {
            parcial = parcial.comDestino(no.get("destino").asString());
        }
        if (no.has("ida")) {
            parcial = parcial.comDatas(LocalDate.parse(no.get("ida").asString()),
                    no.has("volta") ? LocalDate.parse(no.get("volta").asString()) : null);
        }
        return parcial;
    }

    private static Optional<String> comparar(JsonNode esperado, ResultadoDaInterpretacao obtido) {
        String tipo = esperado.get("tipo").asString();
        return switch (tipo) {
            case "PESQUISA" -> compararPesquisa(esperado, obtido);
            case "PERGUNTA" -> obtido instanceof ResultadoDaInterpretacao.Pergunta p
                    && p.campo().name().equals(esperado.get("campo").asString())
                    ? Optional.empty()
                    : Optional.of("esperava pergunta sobre " + esperado.get("campo").asString()
                            + ", veio " + descrever(obtido));
            case "RECUSA" -> obtido instanceof ResultadoDaInterpretacao.Recusa r
                    && r.motivo().name().equals(esperado.get("motivo").asString())
                    ? Optional.empty()
                    : Optional.of("esperava recusa por " + esperado.get("motivo").asString()
                            + ", veio " + descrever(obtido));
            default -> Optional.of("tipo esperado desconhecido: " + tipo);
        };
    }

    private static Optional<String> compararPesquisa(JsonNode esperado, ResultadoDaInterpretacao obtido) {
        if (!(obtido instanceof ResultadoDaInterpretacao.Pesquisa pesquisa)) {
            return Optional.of("esperava pesquisa, veio " + descrever(obtido));
        }
        SearchIntent intencao = pesquisa.intencao();
        List<String> diferencas = new ArrayList<>();

        conferir(diferencas, "origem", esperado.get("origem").asString(), intencao.origin());
        conferir(diferencas, "destino", esperado.get("destino").asString(), intencao.destination());
        conferir(diferencas, "ida", esperado.get("ida").asString(), intencao.departureDate().toString());
        String voltaEsperada = esperado.has("volta") ? esperado.get("volta").asString() : null;
        String voltaObtida = intencao.returnDate() == null ? null : intencao.returnDate().toString();
        conferir(diferencas, "volta", voltaEsperada, voltaObtida);
        conferir(diferencas, "adultos", esperado.get("adultos").asString(),
                String.valueOf(intencao.passengers().adults()));
        conferir(diferencas, "criancas", esperado.get("criancas").asString(),
                String.valueOf(intencao.passengers().children()));
        conferir(diferencas, "bebes", esperado.get("bebes").asString(),
                String.valueOf(intencao.passengers().infants()));
        String cabineEsperada = esperado.has("cabine")
                ? esperado.get("cabine").asString() : CabinClass.ECONOMY.name();
        conferir(diferencas, "cabine", cabineEsperada, intencao.cabin().name());

        return diferencas.isEmpty() ? Optional.empty() : Optional.of(String.join("; ", diferencas));
    }

    private static void conferir(List<String> diferencas, String campo, String esperado, String obtido) {
        if (esperado == null ? obtido != null : !esperado.equals(obtido)) {
            diferencas.add(campo + ": esperava " + esperado + ", veio " + obtido);
        }
    }

    private static String descrever(ResultadoDaInterpretacao resultado) {
        return switch (resultado) {
            case ResultadoDaInterpretacao.Pesquisa p -> "pesquisa " + p.intencao().origin() + "→"
                    + p.intencao().destination() + " " + p.intencao().departureDate()
                    + (p.intencao().returnDate() == null ? "" : " volta " + p.intencao().returnDate());
            case ResultadoDaInterpretacao.Pergunta p -> "pergunta sobre " + p.campo();
            case ResultadoDaInterpretacao.Recusa r -> "recusa por " + r.motivo();
        };
    }
}
