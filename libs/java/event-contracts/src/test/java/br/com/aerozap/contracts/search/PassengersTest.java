package br.com.aerozap.contracts.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Passengers")
class PassengersTest {

    @Test
    @DisplayName("exige pelo menos um adulto")
    void exigeAdulto() {
        assertThatThrownBy(() -> new Passengers(0, 2, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adults");
    }

    @Test
    @DisplayName("não aceita mais bebês que adultos: bebê viaja no colo")
    void bebeSemColoNaoCabe() {
        assertThatThrownBy(() -> new Passengers(1, 0, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("colo");
        assertThat(new Passengers(2, 0, 2).total()).isEqualTo(4);
    }

    @Test
    @DisplayName("respeita o limite de nove pessoas por pesquisa")
    void respeitaOLimiteDaPesquisa() {
        assertThat(new Passengers(9, 0, 0).total()).isEqualTo(9);
        assertThatThrownBy(() -> new Passengers(5, 5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9");
    }

    @Test
    @DisplayName("não aceita quantidade negativa")
    void naoAceitaNegativo() {
        assertThatThrownBy(() -> new Passengers(1, -1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
