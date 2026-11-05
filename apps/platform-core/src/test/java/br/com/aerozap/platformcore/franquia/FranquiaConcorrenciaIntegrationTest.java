package br.com.aerozap.platformcore.franquia;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.contracts.usage.ReleaseReason;
import br.com.aerozap.platformcore.AbstractIntegrationTest;
import br.com.aerozap.testes.concorrencia.ArenaDeConcorrencia;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O cenário obrigatório do marco, agora contra o serviço de verdade.
 *
 * <p>A invariante foi escrita na AZ-018, antes de existir implementação, contra um SQL do próprio
 * teste. Este arquivo é o momento em que ela encontra o código: as asserções são as mesmas, o
 * caminho agora é HTTP → serviço → PostgreSQL.
 */
class FranquiaConcorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final int CONCORRENTES = 32;
    private static final int RODADAS = 3;

    private Resposta reservar(UUID usuario, String pesquisa, String chave) {
        return postar("/internal/usage/reservations", Map.of(
                "userId", usuario.toString(), "searchId", pesquisa, "idempotencyKey", chave));
    }

    private int saldoDe(UUID usuario) {
        return (Integer) obter("/internal/usage/saldo", "X-Usuario-Id", usuario.toString())
                .dados().get("restantes");
    }

    @Test
    @DisplayName("32 pesquisas simultâneas com saldo 5: exatamente 5 reservas, saldo zerado")
    void autorizaExatamenteOSaldo() {
        UUID usuario = UUID.randomUUID();

        var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES, indice ->
                reservar(usuario, "busca-" + indice, "chave-" + indice).status());

        assertThat(resultado.totalDeFalhas()).as(resultado.resumo()).isZero();
        assertThat(resultado.sucessosQue(status -> status == 200))
                .as("nem uma consulta a mais do que a franquia. " + resultado.resumo())
                .isEqualTo(5);
        assertThat(resultado.sucessosQue(status -> status == 409)).isEqualTo(CONCORRENTES - 5);
        assertThat(saldoDe(usuario)).isZero();
    }

    @Test
    @DisplayName("a última consulta disputada por 32 requisições: exatamente uma é autorizada")
    void apenasUmaNaUltimaConsulta() {
        for (int rodada = 1; rodada <= RODADAS; rodada++) {
            UUID usuario = UUID.randomUUID();
            // Gasta quatro das cinco: sobra exatamente uma para a disputa.
            for (int i = 1; i <= 4; i++) {
                reservar(usuario, "gasto-" + i, "gasto-" + i);
            }

            var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES, indice ->
                    reservar(usuario, "disputa-" + indice, "disputa-" + indice).status());

            assertThat(resultado.totalDeFalhas()).as(resultado.resumo()).isZero();
            assertThat(resultado.sucessosQue(status -> status == 200))
                    .as("rodada " + rodada + ": uma só. " + resultado.resumo())
                    .isEqualTo(1);
            assertThat(saldoDe(usuario)).isZero();
        }
    }

    @Test
    @DisplayName("a mesma chave de idempotência em 32 requisições simultâneas debita uma vez")
    void mesmaChaveSimultanea() {
        UUID usuario = UUID.randomUUID();

        var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES, indice ->
                reservar(usuario, "mesma-busca", "mesma-chave").dados().get("reservationId"));

        assertThat(resultado.totalDeFalhas()).as(resultado.resumo()).isZero();
        assertThat(resultado.sucessos()).as("todas devolvem a MESMA reserva")
                .doesNotHaveDuplicates().hasSize(1);
        assertThat(saldoDe(usuario))
                .as("uma reserva, um débito, mesmo com 32 chamadas idênticas ao mesmo tempo")
                .isEqualTo(4);
    }

    @Test
    @DisplayName("liberações simultâneas da mesma reserva devolvem uma consulta, não 32")
    void liberacoesSimultaneas() {
        UUID usuario = UUID.randomUUID();
        String reservaId = (String) reservar(usuario, "busca-1", "chave-1")
                .dados().get("reservationId");

        var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES, indice ->
                postar("/internal/usage/reservations/" + reservaId + "/release",
                        Map.of("reason", ReleaseReason.CANCELLED.name())).status());

        assertThat(resultado.totalDeFalhas()).as(resultado.resumo()).isZero();
        assertThat(saldoDe(usuario))
                .as("o saldo não pode passar da franquia nem com 32 liberações ao mesmo tempo")
                .isEqualTo(5);
    }
}
