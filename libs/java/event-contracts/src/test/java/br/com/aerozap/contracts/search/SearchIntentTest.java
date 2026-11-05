package br.com.aerozap.contracts.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SearchIntent")
class SearchIntentTest {

    private static final LocalDate IDA = LocalDate.of(2026, 7, 10);

    @Test
    @DisplayName("aplica os padrões: econômica, BRL e pt-BR")
    void aplicaOsPadroes() {
        SearchIntent intencao = SearchIntent.ida("BSB", "LIS", IDA);

        assertThat(intencao.cabin()).isEqualTo(CabinClass.ECONOMY);
        assertThat(intencao.currency()).isEqualTo("BRL");
        assertThat(intencao.locale()).isEqualTo("pt-BR");
        assertThat(intencao.preferredAirlines()).isEmpty();
        assertThat(intencao.tripType()).isEqualTo(TripType.ONE_WAY);
    }

    @Test
    @DisplayName("a data de volta é o que define ida e volta")
    void dataDeVoltaDefineIdaEVolta() {
        SearchIntent intencao = new SearchIntent("BSB", "LIS", IDA, IDA.plusDays(10),
                Passengers.umAdulto(), null, null, null, null, null, null);

        assertThat(intencao.tripType()).isEqualTo(TripType.ROUND_TRIP);
    }

    @Test
    @DisplayName("a lista de companhias fica imutável, mesmo que quem criou altere a original")
    void companhiasFicamImutaveis() {
        List<String> companhias = new java.util.ArrayList<>(List.of("TP"));
        SearchIntent intencao = new SearchIntent("BSB", "LIS", IDA, null, Passengers.umAdulto(),
                null, null, null, companhias, null, null);
        companhias.add("LA");

        assertThat(intencao.preferredAirlines()).containsExactly("TP");
        assertThatThrownBy(() -> intencao.preferredAirlines().add("AF"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("recusa origem igual ao destino")
    void recusaOrigemIgualAoDestino() {
        assertThatThrownBy(() -> SearchIntent.ida("BSB", "BSB", IDA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não podem ser iguais");
    }

    @Test
    @DisplayName("recusa código de aeroporto fora do formato IATA")
    void recusaIataForaDoFormato() {
        assertThatThrownBy(() -> SearchIntent.ida("bsb", "LIS", IDA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IATA");
        assertThatThrownBy(() -> SearchIntent.ida("BRASILIA", "LIS", IDA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("recusa volta antes da ida")
    void recusaVoltaAntesDaIda() {
        assertThatThrownBy(() -> new SearchIntent("BSB", "LIS", IDA, IDA.minusDays(1),
                Passengers.umAdulto(), null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("returnDate");
    }

    @Test
    @DisplayName("recusa moeda e idioma fora do contrato")
    void recusaMoedaEIdiomaInvalidos() {
        assertThatThrownBy(() -> new SearchIntent("BSB", "LIS", IDA, null, Passengers.umAdulto(),
                null, "REAL", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO 4217");
        assertThatThrownBy(() -> new SearchIntent("BSB", "LIS", IDA, null, Passengers.umAdulto(),
                null, null, null, null, null, "en-US"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pt-BR ou es");
    }

    @Test
    @DisplayName("recusa teto de preço zerado ou negativo")
    void recusaTetoDePrecoInvalido() {
        assertThatThrownBy(() -> new SearchIntent("BSB", "LIS", IDA, null, Passengers.umAdulto(),
                null, null, null, null, BigDecimal.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxPrice");
    }

    @Test
    @DisplayName("não julga se a data é futura: isso é do serviço, não do contrato")
    void naoJulgaDataNoPassado() {
        assertThat(SearchIntent.ida("BSB", "LIS", LocalDate.of(2020, 1, 1))).isNotNull();
    }
}
