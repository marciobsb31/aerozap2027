package br.com.aerozap.platformcore.franquia;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.platformcore.AbstractIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Cenário obrigatório: o worker caiu no meio da pesquisa.
 *
 * <p>A consulta já foi debitada e ninguém vai confirmar nem liberar. Sem o expirador, ela fica
 * presa até a virada do período — e a pessoa não tem como saber disso nem como reclamar com
 * precisão. Por isso o conserto é automático.
 *
 * <p>O teste envelhece a reserva no banco em vez de esperar o relógio: esperar quinze minutos num
 * teste é inviável, e encurtar o prazo por configuração testaria a configuração, não a regra.
 */
class ExpiracaoDeReservasIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpiradorDeReservas expirador;

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("reserva abandonada além do prazo volta para o saldo, marcada como expirada")
    void reservaOrfaVolta() {
        UUID usuario = UUID.randomUUID();
        String reservaId = (String) postar("/internal/usage/reservations", Map.of(
                "userId", usuario.toString(), "searchId", "busca-orfa", "idempotencyKey", "orfa"))
                .dados().get("reservationId");
        assertThat(saldoDe(usuario)).isEqualTo(4);

        envelhecer(reservaId);
        expirador.liberarVencidas();

        assertThat(saldoDe(usuario))
                .as("worker derrubado não pode custar consulta a quem pesquisou")
                .isEqualTo(5);
        assertThat(estadoDe(reservaId))
                .as("o motivo fica registrado: é o que permite ao suporte explicar o saldo")
                .isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("rodar o expirador duas vezes não devolve a consulta duas vezes")
    void expiracaoNaoDuplicaCredito() {
        UUID usuario = UUID.randomUUID();
        String reservaId = (String) postar("/internal/usage/reservations", Map.of(
                "userId", usuario.toString(), "searchId", "busca-orfa", "idempotencyKey", "orfa"))
                .dados().get("reservationId");

        envelhecer(reservaId);
        expirador.liberarVencidas();
        expirador.liberarVencidas();

        assertThat(saldoDe(usuario)).isEqualTo(5);
    }

    @Test
    @DisplayName("reserva já confirmada não é tocada pelo expirador")
    void confirmadaNaoExpira() {
        UUID usuario = UUID.randomUUID();
        String reservaId = (String) postar("/internal/usage/reservations", Map.of(
                "userId", usuario.toString(), "searchId", "busca-ok", "idempotencyKey", "ok"))
                .dados().get("reservationId");
        postar("/internal/usage/reservations/" + reservaId + "/confirm", Map.of());

        envelhecer(reservaId);
        expirador.liberarVencidas();

        assertThat(estadoDe(reservaId)).isEqualTo("CONFIRMED");
        assertThat(saldoDe(usuario))
                .as("consulta confirmada foi usada; não volta por prazo nenhum")
                .isEqualTo(4);
    }

    private void envelhecer(String reservaId) {
        jdbc.sql("update franquia.reserva_de_uso set expira_em = now() - interval '1 hour' "
                        + "where id = :id")
                .param("id", UUID.fromString(reservaId)).update();
    }

    private String estadoDe(String reservaId) {
        return jdbc.sql("select estado from franquia.reserva_de_uso where id = :id")
                .param("id", UUID.fromString(reservaId)).query(String.class).single();
    }

    private int saldoDe(UUID usuario) {
        return (Integer) obter("/internal/usage/saldo", "X-Usuario-Id", usuario.toString())
                .dados().get("restantes");
    }
}
