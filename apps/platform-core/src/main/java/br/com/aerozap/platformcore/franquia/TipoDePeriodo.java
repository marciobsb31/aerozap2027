package br.com.aerozap.platformcore.franquia;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

/**
 * Como a janela da franquia é contada.
 *
 * <p>O PRD deixa isso em aberto (RN-02) com o trimestre-calendário como hipótese. As duas formas
 * existem aqui desde já e a escolha é configuração, não código: trocar depois seria migrar dados
 * de período, que é caro, e a diferença entre as duas é de uma função.
 */
public enum TipoDePeriodo {

    /**
     * Trimestre do calendário: todo mundo vira no mesmo dia (1º de janeiro, abril, julho, outubro).
     * Simples de explicar e de conferir; concentra a virada, o que é um pico previsível.
     */
    TRIMESTRE_CALENDARIO {
        @Override
        public Janela janelaDe(Instant momento, Instant inicioDaConta, ZoneId fuso) {
            ZonedDateTime agora = momento.atZone(fuso);
            int mesInicial = ((agora.get(IsoFields.QUARTER_OF_YEAR) - 1) * 3) + 1;
            ZonedDateTime inicio = agora.withMonth(mesInicial).withDayOfMonth(1)
                    .toLocalDate().atStartOfDay(fuso);
            return new Janela(inicio.toInstant(), inicio.plusMonths(3).toInstant());
        }
    },

    /**
     * Janela móvel de três meses contada do cadastro: cada pessoa vira no seu próprio dia.
     * Distribui a virada, mas é mais difícil de explicar a quem pergunta "quando meu saldo volta?".
     */
    JANELA_MOVEL {
        @Override
        public Janela janelaDe(Instant momento, Instant inicioDaConta, ZoneId fuso) {
            ZonedDateTime cadastro = inicioDaConta.atZone(fuso);
            ZonedDateTime inicio = cadastro;
            while (!inicio.plusMonths(3).toInstant().isAfter(momento)) {
                inicio = inicio.plusMonths(3);
            }
            return new Janela(inicio.toInstant(), inicio.plusMonths(3).toInstant());
        }
    };

    /**
     * @param inicioDaConta usado só pela janela móvel; o trimestre-calendário o ignora
     */
    public abstract Janela janelaDe(Instant momento, Instant inicioDaConta, ZoneId fuso);

    /** Início inclusivo, fim exclusivo — para que uma virada não tenha nem buraco nem sobreposição. */
    public record Janela(Instant inicio, Instant fim) {

        public boolean contem(Instant momento) {
            return !momento.isBefore(inicio) && momento.isBefore(fim);
        }
    }
}
