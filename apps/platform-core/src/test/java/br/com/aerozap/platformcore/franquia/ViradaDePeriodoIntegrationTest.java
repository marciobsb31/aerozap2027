package br.com.aerozap.platformcore.franquia;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.platformcore.AbstractIntegrationTest;
import br.com.aerozap.testes.concorrencia.ArenaDeConcorrencia;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

/**
 * Atravessa a virada do trimestre com o relógio andando.
 *
 * <p>As datas ficam em 2100 de propósito: o banco é compartilhado com os outros testes, e uma
 * reserva com prazo no futuro distante nunca é apanhada pelo expirador de outro contexto. O
 * expirador deste contexto fica parado para não enxergar as reservas dos outros com o relógio
 * adiantado; os testes o chamam diretamente.
 */
@Import(ViradaDePeriodoIntegrationTest.ConfiguracaoDoRelogio.class)
@TestPropertySource(properties = "aerozap.franquia.intervalo-de-expiracao-ms=86400000")
class ViradaDePeriodoIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Instant ULTIMO_SEGUNDO = ZonedDateTime.of(2100, 12, 31, 23, 59, 59, 0, FUSO).toInstant();
    private static final Instant PRIMEIRO_SEGUNDO = ZonedDateTime.of(2101, 1, 1, 0, 0, 0, 0, FUSO).toInstant();

    /** Relógio que o teste move. Fora dos testes desta classe, é o relógio de verdade. */
    static class RelogioDeTeste extends Clock {
        volatile Instant fixo;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return fixo != null ? fixo : Instant.now();
        }
    }

    @TestConfiguration
    static class ConfiguracaoDoRelogio {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste();
        }
    }

    @Autowired
    private RelogioDeTeste relogio;

    @Autowired
    private ExpiradorDeReservas expirador;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void voltarOTempo() {
        relogio.fixo = null;
    }

    private Resposta reservar(UUID usuario, String chave) {
        return postar("/internal/usage/reservations", Map.of(
                "userId", usuario.toString(), "searchId", "busca-" + chave, "idempotencyKey", chave));
    }

    private Map<String, Object> saldo(UUID usuario) {
        return obter("/internal/usage/saldo", "X-Usuario-Id", usuario.toString()).dados();
    }

    private void gastarTudo(UUID usuario) {
        for (int i = 1; i <= 5; i++) {
            assertThat(reservar(usuario, "gasto-" + i).status()).isEqualTo(200);
        }
    }

    private long periodosDe(UUID usuario) {
        return jdbc.sql("select count(*) from franquia.periodo_de_uso where usuario_id = :usuario")
                .param("usuario", usuario).query(Long.class).single();
    }

    @Test
    @DisplayName("a virada no fuso do Brasil repõe o saldo uma vez, e nem um segundo antes")
    void viradaRepoeOSaldo() {
        UUID usuario = UUID.randomUUID();
        relogio.fixo = ULTIMO_SEGUNDO;
        gastarTudo(usuario);

        assertThat(reservar(usuario, "sexta").status()).as("sem saldo no último segundo").isEqualTo(409);
        assertThat(saldo(usuario).get("restantes")).isEqualTo(0);
        String periodoAntigo = (String) saldo(usuario).get("periodoId");

        relogio.fixo = PRIMEIRO_SEGUNDO;
        Map<String, Object> novo = saldo(usuario);

        assertThat(novo.get("restantes")).isEqualTo(5);
        assertThat(novo.get("periodoId")).isNotEqualTo(periodoAntigo);
        assertThat(saldo(usuario).get("periodoId")).as("ler de novo não abre outro período").isEqualTo(novo.get("periodoId"));
        assertThat(periodosDe(usuario)).isEqualTo(2);
    }

    @Test
    @DisplayName("32 pesquisas simultâneas logo depois da virada: um período e cinco reservas")
    void corridaNaVirada() {
        UUID usuario = UUID.randomUUID();
        relogio.fixo = ULTIMO_SEGUNDO;
        gastarTudo(usuario);
        relogio.fixo = PRIMEIRO_SEGUNDO;

        var resultado = ArenaDeConcorrencia.disparar(32, indice -> reservar(usuario, "corrida-" + indice).status());

        assertThat(resultado.totalDeFalhas()).as(resultado.resumo()).isZero();
        assertThat(resultado.sucessosQue(status -> status == 200)).isEqualTo(5);
        assertThat(resultado.sucessosQue(status -> status == 409)).isEqualTo(27);
        assertThat(periodosDe(usuario)).as("criar um segundo dobraria a franquia").isEqualTo(2);
        assertThat(saldo(usuario).get("restantes")).isEqualTo(0);
    }

    @Test
    @DisplayName("reserva de antes da virada devolvida depois volta ao período dela, não ao novo")
    void liberacaoDepoisDaVirada() {
        UUID usuario = UUID.randomUUID();
        relogio.fixo = ULTIMO_SEGUNDO;
        String reserva = reservar(usuario, "antiga").texto("reservationId");

        relogio.fixo = PRIMEIRO_SEGUNDO;
        assertThat(reservar(usuario, "nova").status()).isEqualTo(200);
        assertThat(saldo(usuario).get("restantes")).isEqualTo(4);

        Resposta liberada = postar("/internal/usage/reservations/" + reserva + "/release",
                Map.of("reason", "SEARCH_FAILED"));

        assertThat(liberada.status()).isEqualTo(200);
        assertThat(saldo(usuario).get("restantes"))
                .as("o saldo do trimestre novo não ganha consulta do trimestre velho").isEqualTo(4);
    }

    @Test
    @DisplayName("worker caiu e a reserva ficou órfã: o prazo passa e a consulta volta sozinha")
    void reservaOrfaVoltaComOTempo() {
        UUID usuario = UUID.randomUUID();
        Instant inicio = ULTIMO_SEGUNDO.minusSeconds(86_400);
        relogio.fixo = inicio;
        reservar(usuario, "orfa");
        assertThat(saldo(usuario).get("restantes")).isEqualTo(4);

        expirador.liberarVencidas();
        assertThat(saldo(usuario).get("restantes")).as("dentro do prazo, continua reservada").isEqualTo(4);

        relogio.fixo = inicio.plusSeconds(3_600 * 12);
        expirador.liberarVencidas();
        expirador.liberarVencidas();

        assertThat(saldo(usuario).get("restantes")).as("volta uma vez, mesmo rodando duas").isEqualTo(5);
        assertThat(jdbc.sql("select estado from franquia.reserva_de_uso where usuario_id = :usuario")
                .param("usuario", usuario).query(String.class).list()).isEqualTo(List.of("EXPIRED"));
    }
}
