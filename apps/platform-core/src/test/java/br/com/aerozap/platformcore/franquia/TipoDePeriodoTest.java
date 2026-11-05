package br.com.aerozap.platformcore.franquia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A virada do período é onde o saldo é reposto — e repor duas vezes, ou nenhuma, é dinheiro.
 * Por isso os limites são testados na borda, e não no meio do trimestre.
 */
class TipoDePeriodoTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    private static Instant em(int ano, int mes, int dia, int hora) {
        return ZonedDateTime.of(ano, mes, dia, hora, 0, 0, 0, SP).toInstant();
    }

    @Test
    @DisplayName("trimestre-calendário: 1º de janeiro a 31 de março")
    void primeiroTrimestre() {
        var janela = TipoDePeriodo.TRIMESTRE_CALENDARIO.janelaDe(em(2026, 2, 14, 10), Instant.EPOCH, SP);

        assertThat(janela.inicio()).isEqualTo(em(2026, 1, 1, 0));
        assertThat(janela.fim()).isEqualTo(em(2026, 4, 1, 0));
    }

    @Test
    @DisplayName("o último instante do trimestre ainda pertence a ele; o primeiro do seguinte, não")
    void bordaDaVirada() {
        var primeiro = TipoDePeriodo.TRIMESTRE_CALENDARIO.janelaDe(em(2026, 3, 31, 23), Instant.EPOCH, SP);
        var segundo = TipoDePeriodo.TRIMESTRE_CALENDARIO.janelaDe(em(2026, 4, 1, 0), Instant.EPOCH, SP);

        assertThat(primeiro.fim()).isEqualTo(segundo.inicio());
        assertThat(primeiro.contem(em(2026, 3, 31, 23))).isTrue();
        assertThat(primeiro.contem(segundo.inicio()))
                .as("fim exclusivo: sem buraco e sem sobreposição entre dois períodos")
                .isFalse();
        assertThat(segundo.contem(segundo.inicio())).isTrue();
    }

    @Test
    @DisplayName("os quatro trimestres começam nos meses certos")
    void quatroTrimestres() {
        assertThat(inicioDoTrimestreEm(2026, 1)).isEqualTo(em(2026, 1, 1, 0));
        assertThat(inicioDoTrimestreEm(2026, 5)).isEqualTo(em(2026, 4, 1, 0));
        assertThat(inicioDoTrimestreEm(2026, 9)).isEqualTo(em(2026, 7, 1, 0));
        assertThat(inicioDoTrimestreEm(2026, 12)).isEqualTo(em(2026, 10, 1, 0));
    }

    @Test
    @DisplayName("janela móvel: conta do cadastro, de três em três meses")
    void janelaMovel() {
        Instant cadastro = em(2026, 2, 14, 9);

        var primeira = TipoDePeriodo.JANELA_MOVEL.janelaDe(em(2026, 3, 1, 12), cadastro, SP);
        assertThat(primeira.inicio()).isEqualTo(cadastro);
        assertThat(primeira.fim()).isEqualTo(em(2026, 5, 14, 9));

        var terceira = TipoDePeriodo.JANELA_MOVEL.janelaDe(em(2026, 9, 1, 12), cadastro, SP);
        assertThat(terceira.inicio()).isEqualTo(em(2026, 8, 14, 9));
        assertThat(terceira.fim()).isEqualTo(em(2026, 11, 14, 9));
        assertThat(terceira.contem(em(2026, 9, 1, 12))).isTrue();
    }

    @Test
    @DisplayName("no próprio instante do cadastro, a janela móvel já vale")
    void janelaMovelNoInstanteDoCadastro() {
        Instant cadastro = em(2026, 2, 14, 9);
        var janela = TipoDePeriodo.JANELA_MOVEL.janelaDe(cadastro, cadastro, SP);
        assertThat(janela.contem(cadastro)).isTrue();
    }

    private static Instant inicioDoTrimestreEm(int ano, int mes) {
        return TipoDePeriodo.TRIMESTRE_CALENDARIO.janelaDe(em(ano, mes, 15, 12), Instant.EPOCH, SP)
                .inicio();
    }
}
