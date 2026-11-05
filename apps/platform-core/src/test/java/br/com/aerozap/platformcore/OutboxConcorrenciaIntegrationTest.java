package br.com.aerozap.platformcore;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.contracts.EventEnvelope;
import br.com.aerozap.foundation.outbox.OutboxService;
import br.com.aerozap.testes.concorrencia.ArenaDeConcorrencia;
import br.com.aerozap.testes.dados.DadosSinteticos;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * O outbox sob disputa: código de produção, PostgreSQL real, N gravações ao mesmo tempo.
 *
 * <p>Duas invariantes, e as duas só se verificam com simultaneidade. A primeira é que o mesmo
 * evento gravado N vezes ao mesmo tempo produz uma linha e N-1 recusas do banco — é a chave
 * primária decidindo, não a aplicação. A segunda é que N eventos distintos produzem exatamente N
 * linhas: nenhuma se perde pelo caminho, que é o defeito silencioso do outbox — o evento some, a
 * mudança de negócio fica gravada e ninguém percebe até o dado divergir.
 */
class OutboxConcorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final int CONCORRENTES = 24;

    @Autowired
    private OutboxService outbox;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TransactionTemplate transacao;

    private EventEnvelope evento(UUID id, String agregado) {
        return new EventEnvelope(id, "aerozap.teste.concorrencia", 1, Instant.now(),
                "corr-" + agregado, agregado, "{\"teste\":true}");
    }

    private long linhasDoTipoDeTeste() {
        return jdbc.sql("select count(*) from outbox_event where event_type = :t")
                .param("t", "aerozap.teste.concorrencia").query(Long.class).single();
    }

    @Test
    @DisplayName("o mesmo evento gravado 24 vezes ao mesmo tempo vira uma linha só")
    void mesmoEventoUmaLinha() {
        UUID id = UUID.randomUUID();
        long antes = linhasDoTipoDeTeste();

        var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES, indice -> {
            try {
                transacao.executeWithoutResult(status -> outbox.append(evento(id, "agregado-unico")));
                return true;
            } catch (RuntimeException e) {
                // Conflito de chave primária: é o banco recusando a duplicata, e é o resultado certo.
                return false;
            }
        });

        assertThat(resultado.sucessosQue(Boolean.TRUE::equals))
                .as("exatamente uma gravação pode vencer. " + resultado.resumo())
                .isEqualTo(1);
        assertThat(linhasDoTipoDeTeste() - antes).isEqualTo(1);
    }

    @Test
    @DisplayName("24 eventos distintos simultâneos geram 24 linhas: nenhuma gravação se perde")
    void eventosDistintosNaoSePerdem() {
        var dados = DadosSinteticos.padrao();
        long antes = linhasDoTipoDeTeste();

        var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES, indice -> {
            UUID id = dadosId(dados);
            transacao.executeWithoutResult(status -> outbox.append(evento(id, "agregado-" + indice)));
            return id;
        });

        assertThat(resultado.totalDeFalhas()).as(resultado.resumo()).isZero();
        assertThat(resultado.sucessos()).doesNotHaveDuplicates();
        assertThat(linhasDoTipoDeTeste() - antes).isEqualTo(CONCORRENTES);
    }

    /** O gerador sintético não é seguro para várias threads; o sorteio fica sob o mesmo monitor. */
    private synchronized UUID dadosId(DadosSinteticos dados) {
        return dados.identificador();
    }
}
