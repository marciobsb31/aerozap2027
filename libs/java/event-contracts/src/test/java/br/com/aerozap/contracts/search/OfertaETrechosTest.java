package br.com.aerozap.contracts.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Oferta, itinerário e trechos")
class OfertaETrechosTest {

    private static final Instant PARTIDA = Instant.parse("2026-07-10T22:15:00Z");

    private static Segment trecho(String origem, String destino, Instant partida, Instant chegada) {
        return new Segment(origem, destino, partida, chegada, "TP", "088", CabinClass.ECONOMY, "A339");
    }

    @Test
    @DisplayName("itinerário com conexão conta uma escala e soma a duração total")
    void itinerarioComConexao() {
        Segment primeiro = trecho("GRU", "LIS", PARTIDA, PARTIDA.plus(Duration.ofHours(10)));
        Segment segundo = trecho("LIS", "MAD", PARTIDA.plus(Duration.ofHours(12)),
                PARTIDA.plus(Duration.ofHours(13)));

        Itinerary itinerario = new Itinerary(List.of(primeiro, segundo));

        assertThat(itinerario.origin()).isEqualTo("GRU");
        assertThat(itinerario.destination()).isEqualTo("MAD");
        assertThat(itinerario.stops()).isEqualTo(1);
        assertThat(itinerario.totalDuration()).isEqualTo(Duration.ofHours(13));
    }

    @Test
    @DisplayName("recusa itinerário cujos trechos não se encadeiam")
    void recusaTrechosQueNaoSeEncadeiam() {
        Segment primeiro = trecho("GRU", "LIS", PARTIDA, PARTIDA.plus(Duration.ofHours(10)));
        Segment solto = trecho("CDG", "MAD", PARTIDA.plus(Duration.ofHours(12)),
                PARTIDA.plus(Duration.ofHours(13)));

        assertThatThrownBy(() -> new Itinerary(List.of(primeiro, solto)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não se encadeiam");
    }

    @Test
    @DisplayName("recusa conexão que parte antes de o trecho anterior chegar")
    void recusaConexaoImpossivel() {
        Segment primeiro = trecho("GRU", "LIS", PARTIDA, PARTIDA.plus(Duration.ofHours(10)));
        Segment adiantado = trecho("LIS", "MAD", PARTIDA.plus(Duration.ofHours(9)),
                PARTIDA.plus(Duration.ofHours(11)));

        assertThatThrownBy(() -> new Itinerary(List.of(primeiro, adiantado)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("oferta de ida e volta tem dois itinerários e soma as escalas dos dois")
    void ofertaDeIdaEVolta() {
        Itinerary ida = new Itinerary(List.of(trecho("GRU", "LIS", PARTIDA, PARTIDA.plusSeconds(36000))));
        Itinerary volta = new Itinerary(List.of(
                trecho("LIS", "MAD", PARTIDA.plusSeconds(100000), PARTIDA.plusSeconds(104000)),
                trecho("MAD", "GRU", PARTIDA.plusSeconds(110000), PARTIDA.plusSeconds(140000))));

        FlightOffer oferta = new FlightOffer("of_1", "TRAVELPAYOUTS", false,
                Money.de("4820.35", "BRL"), List.of(ida, volta), null, null, Instant.now());

        assertThat(oferta.tripType()).isEqualTo(TripType.ROUND_TRIP);
        assertThat(oferta.totalStops()).isEqualTo(1);
    }

    @Test
    @DisplayName("recusa oferta sem fonte: preço sem provedor não é exibível")
    void recusaOfertaSemProvedor() {
        Itinerary ida = new Itinerary(List.of(trecho("GRU", "LIS", PARTIDA, PARTIDA.plusSeconds(36000))));

        assertThatThrownBy(() -> new FlightOffer("of_1", "  ", false, Money.de("100", "BRL"),
                List.of(ida), null, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem fonte");
    }

    @Test
    @DisplayName("recusa mais de dois itinerários: v0 não cobre múltiplos destinos")
    void recusaTresItinerarios() {
        Itinerary ida = new Itinerary(List.of(trecho("GRU", "LIS", PARTIDA, PARTIDA.plusSeconds(36000))));

        assertThatThrownBy(() -> new FlightOffer("of_1", "TP", false, Money.de("100", "BRL"),
                List.of(ida, ida, ida), null, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
