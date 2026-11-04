package br.com.aerozap.contracts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.aerozap.contracts.events.MessageReceived;
import br.com.aerozap.contracts.events.PriceTargetReached;
import br.com.aerozap.contracts.events.SearchCompleted;
import br.com.aerozap.contracts.events.SearchFailed;
import br.com.aerozap.contracts.events.SearchRequested;
import br.com.aerozap.contracts.events.UsageConfirmed;
import br.com.aerozap.contracts.events.UsageReleased;
import br.com.aerozap.contracts.events.UsageReserved;
import br.com.aerozap.contracts.search.FlightOffer;
import br.com.aerozap.contracts.search.ProviderResult;
import br.com.aerozap.contracts.search.SearchIntent;
import br.com.aerozap.contracts.usage.ReservationRequest;
import br.com.aerozap.contracts.usage.ReservationResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Teste de contrato: os exemplos publicados em {@code docs/api-contracts/exemplos} são lidos de
 * verdade contra os tipos desta biblioteca.
 *
 * <p>Por que isto importa: documentação de contrato apodrece em silêncio. Aqui, um exemplo que
 * deixa de casar com o tipo — ou uma regra que some do tipo e deixa de rejeitar o exemplo
 * inválido — quebra o build. O arquivo é a fonte que o front-end e os provedores leem; o teste
 * garante que ele continua verdadeiro.
 *
 * <p>Convenção: cada pasta é um contrato, {@code valido-*.json} precisa ser aceito e
 * {@code invalido-*.json} precisa ser recusado.
 */
@DisplayName("Contratos publicados")
class ContratosPublicadosTest {

    /** Pasta de exemplos por contrato, na ordem em que entram na documentação. */
    private static final Map<String, Class<?>> CONTRATOS = new LinkedHashMap<>();

    static {
        CONTRATOS.put("search-intent", SearchIntent.class);
        CONTRATOS.put("flight-offer", FlightOffer.class);
        CONTRATOS.put("provider-result", ProviderResult.class);
        CONTRATOS.put("usage-reservation-request", ReservationRequest.class);
        CONTRATOS.put("usage-reservation-response", ReservationResponse.class);
        CONTRATOS.put("event-envelope", EventEnvelope.class);
        CONTRATOS.put("event-search-requested", SearchRequested.class);
        CONTRATOS.put("event-search-completed", SearchCompleted.class);
        CONTRATOS.put("event-search-failed", SearchFailed.class);
        CONTRATOS.put("event-usage-reserved", UsageReserved.class);
        CONTRATOS.put("event-usage-confirmed", UsageConfirmed.class);
        CONTRATOS.put("event-usage-released", UsageReleased.class);
        CONTRATOS.put("event-price-target-reached", PriceTargetReached.class);
        CONTRATOS.put("event-message-received", MessageReceived.class);
    }

    private static final ObjectMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /** Exemplo a verificar: arquivo, tipo esperado e se deve ser aceito. */
    private record Exemplo(Path arquivo, Class<?> tipo, boolean valido) {
        @Override
        public String toString() {
            return arquivo.getParent().getFileName() + "/" + arquivo.getFileName();
        }
    }

    private static Path raizDosExemplos() {
        Path atual = Path.of("").toAbsolutePath();
        while (atual != null) {
            Path candidato = atual.resolve("docs/api-contracts/exemplos");
            if (Files.isDirectory(candidato)) {
                return candidato;
            }
            atual = atual.getParent();
        }
        throw new IllegalStateException("não encontrei docs/api-contracts/exemplos a partir de "
                + Path.of("").toAbsolutePath());
    }

    static Stream<Exemplo> exemplos() throws IOException {
        Path raiz = raizDosExemplos();
        List<Exemplo> encontrados = new ArrayList<>();
        for (Map.Entry<String, Class<?>> contrato : CONTRATOS.entrySet()) {
            Path pasta = raiz.resolve(contrato.getKey());
            try (Stream<Path> arquivos = Files.list(pasta)) {
                arquivos.filter(a -> a.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .forEach(a -> encontrados.add(new Exemplo(a, contrato.getValue(),
                                a.getFileName().toString().startsWith("valido"))));
            }
        }
        return encontrados.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exemplos")
    @DisplayName("exemplo publicado casa com o tipo")
    void exemploCasaComOTipo(Exemplo exemplo) throws IOException {
        String conteudo = Files.readString(exemplo.arquivo());

        if (exemplo.valido()) {
            Object lido = JSON.readValue(conteudo, exemplo.tipo());
            assertThat(lido).isNotNull();
            // Ida e volta: o que sai precisa voltar a entrar, senão o contrato só vale num sentido.
            String reescrito = JSON.writeValueAsString(lido);
            assertThatCode(() -> JSON.readValue(reescrito, exemplo.tipo())).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> JSON.readValue(conteudo, exemplo.tipo()))
                    .as("o exemplo inválido precisa ser recusado pelo tipo")
                    .isInstanceOf(Exception.class);
        }
    }

    @Test
    @DisplayName("todo contrato publica pelo menos um exemplo válido e um inválido")
    void todoContratoTemExemploValidoEInvalido() throws IOException {
        Path raiz = raizDosExemplos();
        for (String contrato : CONTRATOS.keySet()) {
            Path pasta = raiz.resolve(contrato);
            assertThat(pasta).as("pasta de exemplos de %s", contrato).isDirectory();
            try (Stream<Path> arquivos = Files.list(pasta)) {
                List<String> nomes = arquivos.map(a -> a.getFileName().toString()).sorted().toList();
                assertThat(nomes).as("exemplos válidos de %s", contrato)
                        .anyMatch(n -> n.startsWith("valido"));
                assertThat(nomes).as("exemplos inválidos de %s", contrato)
                        .anyMatch(n -> n.startsWith("invalido"));
            }
        }
    }

    @Test
    @DisplayName("nenhuma pasta de exemplos ficou fora do teste")
    void nenhumaPastaFicaForaDoTeste() throws IOException {
        Path raiz = raizDosExemplos();
        try (Stream<Path> pastas = Files.list(raiz)) {
            List<String> nomes = pastas.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString()).sorted().toList();
            assertThat(nomes)
                    .as("toda pasta de exemplo precisa de um tipo correspondente em CONTRATOS")
                    .containsExactlyInAnyOrderElementsOf(CONTRATOS.keySet());
        }
    }
}
