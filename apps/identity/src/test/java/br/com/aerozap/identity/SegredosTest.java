package br.com.aerozap.identity;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.identity.seguranca.Segredos;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SegredosTest {

    @Test
    @DisplayName("tokens não se repetem e são longos o bastante para não serem adivinhados")
    void tokensUnicos() {
        Set<String> gerados = new HashSet<>();
        for (int i = 0; i < 5_000; i++) {
            gerados.add(Segredos.novoToken());
        }
        assertThat(gerados).hasSize(5_000);
        assertThat(gerados.iterator().next()).hasSizeGreaterThanOrEqualTo(43);
    }

    @Test
    @DisplayName("o hash é estável, tem 64 caracteres e não devolve o original")
    void hashEstavel() {
        String token = Segredos.novoToken();
        assertThat(Segredos.hash(token))
                .isEqualTo(Segredos.hash(token))
                .hasSize(64)
                .isNotEqualTo(token)
                .matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("o código numérico tem sempre seis dígitos, inclusive quando começa com zero")
    void codigoComSeisDigitos() {
        for (int i = 0; i < 2_000; i++) {
            assertThat(Segredos.novoCodigoNumerico()).matches("\\d{6}");
        }
    }
}
