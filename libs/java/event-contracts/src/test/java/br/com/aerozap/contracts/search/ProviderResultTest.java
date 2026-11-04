package br.com.aerozap.contracts.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Resultado de provedor")
class ProviderResultTest {

    private static final Instant AGORA = Instant.parse("2026-07-01T13:22:10Z");

    private static FlightOffer oferta(String provedor) {
        Segment trecho = new Segment("GRU", "LIS", AGORA, AGORA.plusSeconds(36000),
                "TP", "088", CabinClass.ECONOMY, null);
        return new FlightOffer("of_1", provedor, false, Money.de("100", "BRL"),
                List.of(new Itinerary(List.of(trecho))), null, null, AGORA);
    }

    @Test
    @DisplayName("falha de provedor é resultado, não exceção")
    void falhaEResultado() {
        ProviderResult resultado = ProviderResult.erro("TRAVELPAYOUTS", ProviderStatus.TIMEOUT,
                "sem resposta em 8s", AGORA);

        assertThat(resultado.status()).isEqualTo(ProviderStatus.TIMEOUT);
        assertThat(resultado.temOfertas()).isFalse();
    }

    @Test
    @DisplayName("OK sem oferta nenhuma é incoerente: existe EMPTY para isso")
    void okSemOfertaEIncoerente() {
        assertThatThrownBy(() -> new ProviderResult("TP", ProviderStatus.OK, List.of(), null, AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMPTY");
    }

    @Test
    @DisplayName("erro com ofertas é incoerente")
    void erroComOfertasEIncoerente() {
        assertThatThrownBy(() -> new ProviderResult("TP", ProviderStatus.ERROR,
                List.of(oferta("TP")), null, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("não aceita oferta atribuída a outro provedor")
    void naoAceitaOfertaDeOutroProvedor() {
        assertThatThrownBy(() -> new ProviderResult("SERPAPI", ProviderStatus.OK,
                List.of(oferta("TRAVELPAYOUTS")), null, AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outro provedor");
    }

    @Test
    @DisplayName("vazio é um resultado legítimo")
    void vazioELegitimo() {
        assertThat(ProviderResult.vazio("TP", AGORA).status()).isEqualTo(ProviderStatus.EMPTY);
    }
}
