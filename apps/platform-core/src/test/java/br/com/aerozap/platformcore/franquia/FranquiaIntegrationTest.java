package br.com.aerozap.platformcore.franquia;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.contracts.usage.ReleaseReason;
import br.com.aerozap.platformcore.AbstractIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** As regras de consumo da franquia (ADR-0004, RF-012 a RF-016), contra PostgreSQL real. */
class FranquiaIntegrationTest extends AbstractIntegrationTest {

    private Resposta reservar(UUID usuario, String pesquisa, String chave) {
        return postar("/internal/usage/reservations", Map.of(
                "userId", usuario.toString(), "searchId", pesquisa, "idempotencyKey", chave));
    }

    private int restantes(Resposta resposta) {
        return (Integer) resposta.dados().get("remaining");
    }

    @Test
    @DisplayName("a primeira reserva cria o período e debita uma consulta das cinco")
    void primeiraReserva() {
        UUID usuario = UUID.randomUUID();

        Resposta resposta = reservar(usuario, "busca-1", "chave-1");

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.dados().get("status")).isEqualTo("RESERVED");
        assertThat(restantes(resposta)).isEqualTo(4);
        assertThat(resposta.dados().get("expiresAt")).isNotNull();
    }

    @Test
    @DisplayName("repetir a chamada com a mesma chave devolve a mesma reserva, sem novo débito")
    void idempotencia() {
        UUID usuario = UUID.randomUUID();

        Resposta primeira = reservar(usuario, "busca-1", "mesma-chave");
        Resposta repetida = reservar(usuario, "busca-1", "mesma-chave");

        assertThat(repetida.dados().get("reservationId"))
                .isEqualTo(primeira.dados().get("reservationId"));
        assertThat(restantes(repetida))
                .as("a repetição não pode custar uma segunda consulta")
                .isEqualTo(4);
    }

    @Test
    @DisplayName("confirmar consome de vez; o saldo não volta")
    void confirmar() {
        UUID usuario = UUID.randomUUID();
        String reservaId = (String) reservar(usuario, "busca-1", "chave-1").dados().get("reservationId");

        Resposta confirmada = postar("/internal/usage/reservations/" + reservaId + "/confirm", Map.of());

        assertThat(confirmada.dados().get("status")).isEqualTo("CONFIRMED");
        assertThat(saldoDe(usuario)).isEqualTo(4);
    }

    @Test
    @DisplayName("falha de provedor libera a reserva e devolve a consulta ao saldo")
    void falhaNaoConsome() {
        UUID usuario = UUID.randomUUID();
        String reservaId = (String) reservar(usuario, "busca-1", "chave-1").dados().get("reservationId");

        Resposta liberada = postar("/internal/usage/reservations/" + reservaId + "/release",
                Map.of("reason", ReleaseReason.SEARCH_FAILED.name()));

        assertThat(liberada.dados().get("status")).isEqualTo("RELEASED");
        assertThat(saldoDe(usuario))
                .as("erro de provedor não pode custar consulta a quem pesquisou")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("liberar duas vezes devolve só uma consulta")
    void liberacaoDupla() {
        UUID usuario = UUID.randomUUID();
        String reservaId = (String) reservar(usuario, "busca-1", "chave-1").dados().get("reservationId");
        var corpo = Map.of("reason", ReleaseReason.CANCELLED.name());

        postar("/internal/usage/reservations/" + reservaId + "/release", corpo);
        postar("/internal/usage/reservations/" + reservaId + "/release", corpo);

        assertThat(saldoDe(usuario))
                .as("o saldo não pode passar da franquia nem com liberação repetida")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("confirmar depois de liberar não derruba o saldo de volta")
    void confirmarDepoisDeLiberar() {
        UUID usuario = UUID.randomUUID();
        String reservaId = (String) reservar(usuario, "busca-1", "chave-1").dados().get("reservationId");

        postar("/internal/usage/reservations/" + reservaId + "/release",
                Map.of("reason", ReleaseReason.CANCELLED.name()));
        Resposta confirmada = postar("/internal/usage/reservations/" + reservaId + "/confirm", Map.of());

        assertThat(confirmada.dados().get("status")).isEqualTo("RELEASED");
        assertThat(saldoDe(usuario)).isEqualTo(5);
    }

    @Test
    @DisplayName("a sexta pesquisa é recusada com 409 e o tipo de franquia esgotada")
    void franquiaEsgotada() {
        UUID usuario = UUID.randomUUID();
        for (int i = 1; i <= 5; i++) {
            assertThat(reservar(usuario, "busca-" + i, "chave-" + i).status()).isEqualTo(200);
        }

        Resposta sexta = reservar(usuario, "busca-6", "chave-6");

        assertThat(sexta.status()).isEqualTo(409);
        assertThat(sexta.corpo().get("type")).isEqualTo("urn:aerozap:problem:usage-exhausted");
        assertThat(sexta.corpo().get("correlationId")).isNotNull();
        assertThat(saldoDe(usuario)).isZero();
    }

    @Test
    @DisplayName("recusada por saldo zero, a tentativa não deixa reserva nem débito para trás")
    void recusaNaoDeixaRastro() {
        UUID usuario = UUID.randomUUID();
        for (int i = 1; i <= 5; i++) {
            reservar(usuario, "busca-" + i, "chave-" + i);
        }
        reservar(usuario, "busca-6", "chave-6");

        // Libera uma: se a tentativa recusada tivesse gravado alguma coisa, o saldo não fecharia.
        String primeira = (String) reservar(usuario, "busca-1", "chave-1").dados().get("reservationId");
        postar("/internal/usage/reservations/" + primeira + "/release",
                Map.of("reason", ReleaseReason.CANCELLED.name()));

        assertThat(saldoDe(usuario)).isEqualTo(1);
    }

    @Test
    @DisplayName("o saldo é por usuário: o consumo de um não aparece no do outro")
    void saldoPorUsuario() {
        UUID ana = UUID.randomUUID();
        UUID bruno = UUID.randomUUID();

        reservar(ana, "busca-1", "chave-1");
        reservar(ana, "busca-2", "chave-2");

        assertThat(saldoDe(ana)).isEqualTo(3);
        assertThat(saldoDe(bruno)).isEqualTo(5);
    }

    private int saldoDe(UUID usuario) {
        Resposta saldo = obter("/internal/usage/saldo", "X-Usuario-Id", usuario.toString());
        return (Integer) saldo.dados().get("restantes");
    }
}
